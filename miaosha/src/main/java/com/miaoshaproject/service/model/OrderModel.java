package com.miaoshaproject.service.model;

import java.math.BigDecimal;

/**
 * @Author: crowsjian
 * @Date: 2019/12/2 22:00
 */
// 用户下单交易模型
public class OrderModel {
    private String id;// 订单号
    private Integer userId; // 用户id
    private Integer itemId; // 商品id
    private Integer amount; // 购买商品数量
    private Integer promoId;// 若非空表示 以秒杀方式下单
    private BigDecimal itemPrice; //购买商品的单价，若promoId非空表示秒杀价格
    private BigDecimal orderPrice; // 购买金额

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public BigDecimal getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(BigDecimal orderPrice) {
        this.orderPrice = orderPrice;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }
}
