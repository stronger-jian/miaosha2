package com.miaoshaproject.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EMBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

/**
 * @Author: crowsjian
 * @Date: 2019/12/3 21:56
 */
@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials="true", allowedHeaders="*")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init(){
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(100);
    }

    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPR_FORMED})
    @ResponseBody
    public CommonReturnType generatetoken(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="promoId")Integer promoId) throws BusinessException {
        // 根据token获取用户登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EMBusinessError.USER_NOT_LOGIN, "用户还未登录，不能下单！");
        }
        UserModel userModel = (UserModel)redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BusinessException(EMBusinessError.USER_NOT_LOGIN, "用户还未登录，不能下单！");
        }
        //
        // 获取秒杀响应令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());

        if(promoToken==null){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "生成令牌失败");
        }
        // 返回对应的结果
        return CommonReturnType.create(promoToken);
    }

    // 封装下单请求
    @RequestMapping(value = "/createorder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPR_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                        @RequestParam(name="promoId" , required = false)Integer promoId,
                                        @RequestParam(name="amount")Integer amount,
                                        @RequestParam(name="promoToken" , required = false)String promoToken) throws BusinessException {
        if(!orderCreateRateLimiter.tryAcquire()){
            throw new BusinessException(EMBusinessError.RATE_LIMIT);
        }
        // 获取用户登录信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EMBusinessError.USER_NOT_LOGIN, "用户还未登录，不能下单！");
        }
        UserModel userModel = (UserModel)redisTemplate.opsForValue().get(token);
        if(userModel==null){
            throw new BusinessException(EMBusinessError.USER_NOT_LOGIN, "用户还未登录，不能下单！");
        }

        // 校验秒杀令牌时否正确
        if(promoId!=null){
            String inRedisPormoToken = (String)redisTemplate.opsForValue().get("promo_token_"+promoId+"userId_"+userModel.getId()+"itemId_"+itemId);
            if(inRedisPormoToken==null){
                throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
            if(!StringUtils.equals(inRedisPormoToken, promoToken)){
                throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }
//      //  Boolean isLogin = (Boolean) this.httpServletRequest.getSession().getAttribute("IS_LOGIN");
//        if(isLogin==null||!isLogin.booleanValue()){
//
//        }
//        UserModel userModel = (UserModel) this.httpServletRequest.getSession().getAttribute("LOGIN_USER");
       // OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId,promoId, amount);

        // 同步调用线程池的 submit 方法
        // 拥塞窗口为20的等待队列，用来队列化泄洪
        Future future = executorService.submit(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                // 加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId, amount);

                // 再去完成对应的下单事务型消息机制
                if(!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId,promoId, amount, stockLogId)){
                    throw new  BusinessException(EMBusinessError.UNKNOW_ERROR, "下单失败");
                }
                return null;
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new  BusinessException(EMBusinessError.UNKNOW_ERROR);
        } catch (ExecutionException e) {
            throw new  BusinessException(EMBusinessError.UNKNOW_ERROR);
        }
//        // 加入库存流水init状态
//        String stockLogId = itemService.initStockLog(itemId, amount);
//
//        // 再去完成对应的下单事务型消息机制
//        if(!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId,promoId, amount, stockLogId)){
//            throw new  BusinessException(EMBusinessError.UNKNOW_ERROR, "下单失败");
//        }
        return CommonReturnType.create(null);
    }
}
