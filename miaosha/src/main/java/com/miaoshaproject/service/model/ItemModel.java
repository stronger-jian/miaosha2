package com.miaoshaproject.service.model;

import org.springframework.beans.factory.InitializingBean;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品模型
 * @Author: crowsjian
 * @Date: 2019/11/30 15:42
 */
public class ItemModel implements Serializable {
    private Integer id;// 主键
    @NotBlank(message = "商品名称不能为空")
    private String title;// 商品名称
    @NotNull(message = "商品价格必须填写")
    @Min(value = 0, message = "商品价格不能小于0元")
    private BigDecimal price;// 商品价格
    @NotNull(message = "商品库存必须填写")
    @Min(value = 0, message = "商品库存不能小于0")
    private Integer stock;// 商品库存
    @NotBlank(message = "商品描述不能为空")
    private String description;// 商品描述
    private Integer sales; // 商品销量
    @NotBlank(message = "商品图片不能为空")
    private String imgUrl;// 商品图片路径
    // 聚合模型 如果promoModel 不为空 表示其拥有还未结束的秒杀活动
    private PromoModel promoModel;


    public PromoModel getPromoModel() {
        return promoModel;
    }

    public void setPromoModel(PromoModel promoModel) {
        this.promoModel = promoModel;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
