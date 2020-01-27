package com.miaoshaproject.service.model;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author: crowsjian
 * @Date: 2019/12/7 9:55
 */
public class PromoModel implements Serializable {
    private Integer id;
    private Integer status; // 秒杀活动状态 1还未开始2进行中 3 已结束
    private String promoName;
    private DateTime startTime;
    private DateTime endTime;
    private Integer itemId;
    private BigDecimal promoPrice;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPromoName() {
        return promoName;
    }

    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getPromoPrice() {
        return promoPrice;
    }

    public void setPromoPrice(BigDecimal promoPrice) {
        this.promoPrice = promoPrice;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(DateTime endTime) {
        this.endTime = endTime;
    }
}
