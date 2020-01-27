package com.miaoshaproject.service.Impl;

import com.miaoshaproject.dao.OrderInfoDOMapper;
import com.miaoshaproject.dao.SequenceDoMapper;
import com.miaoshaproject.dao.StockLogDOMapper;
import com.miaoshaproject.dataobject.OrderInfoDO;
import com.miaoshaproject.dataobject.SequenceDo;
import com.miaoshaproject.dataobject.StockLogDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EMBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.TransactionManagementConfigurationSelector;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Author: crowsjian
 * @Date: 2019/12/2 22:23
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderInfoDOMapper orderInfoDOMapper;

    @Autowired
    private SequenceDoMapper sequenceDoMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException {
        // 1 校验下单状态 ，商品书否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemById(itemId);
        //ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "商品信息不正确");
        }
//       // UserModel userModel = userService.getUserById(userId);
//        UserModel userModel = userService.getUserByIdInCache(userId);
//        if(userModel == null){
//            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "用户信息不正确");
//        }
        if(amount<=0||amount>99){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "数量信息不正确");
        }
        // 校验活动信息
        if(promoId!= null){
            // (1)校验对应活动是否存在这个适用商品
            if(promoId.intValue()!=itemModel.getPromoModel().getId()){
                throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "活动信息不正确");
            }else if(itemModel.getPromoModel().getStatus().intValue()!=2){//活动时否在进行中
                throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "活动还未开始");
            }
        }
        // 2 落单减库存，支付减库存(redis)
        boolean result = itemService.deacreaseStock(itemId, amount);
        if(!result){
            throw new BusinessException(EMBusinessError.STOCK_NOT_ENOUGH);
        }
        // 3 订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        if(promoId!=null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoPrice());
        }else{
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setAmount(amount);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        // 生成订单号
        orderModel.setId(generateOrderNO());
        orderModel.setPromoId(promoId);
        OrderInfoDO orderInfoDO = convertFromModel(orderModel);
        orderInfoDOMapper.insertSelective(orderInfoDO);
        // 加销量
        itemService.increaseSales(itemId, amount);

        // 设置库存 流水状态成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if(stockLogDO == null){
            throw new BusinessException(EMBusinessError.UNKNOW_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        // mq 方法 最近的一个事务 提交成功之后
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//            @Override
//            public void afterCommit() {
//                // 异步更新库存（数据库）
//                boolean mqRsult = itemService.asyncDecreaseStock(itemId, amount);
//            }
//        });
//        // 异步更新库存（数据库）
//        boolean mqRsult = itemService.asyncDecreaseStock(itemId, amount);
//        // 若更新库存失败 库存回滚
//        if(!mqRsult){
//            itemService.increaseSales(itemId, amount);
//            throw new BusinessException(EMBusinessError.MQ_SEND_FAIL);
//        }
        // 4 返回前端
        return orderModel;
    }


    private String generateOrderNO(){
        // 订单号16位
        StringBuilder stringBuilder = new StringBuilder();

        // 前八位年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);
        //  中间六位自增
        int sequence = 0;
        SequenceDo sequenceDo = sequenceDoMapper.selectByName("order_info");
        sequence = sequenceDo.getCurrentValue();
        sequenceDo.setCurrentValue(sequenceDo.getCurrentValue()+sequenceDo.getStep());
        sequenceDoMapper.updateByPrimaryKeySelective(sequenceDo);
        for(int i=0;i<(6-String.valueOf(sequenceDo).length());i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequence);
        // 最后2位为分库分表位
        stringBuilder.append("00");
        return stringBuilder.toString();
    }
    private OrderInfoDO convertFromModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderInfoDO result = new OrderInfoDO();
        BeanUtils.copyProperties(orderModel, result);
        return result;
    }
}
