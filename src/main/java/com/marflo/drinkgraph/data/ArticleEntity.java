package com.marflo.drinkgraph.data;

import org.joda.money.Money;

public class ArticleEntity {

    private Long articleId;
    private String name;
    private String name2;
    private Money price;
    private Double volumeInMl;
    private String salesStart;
    private Double alcoholPercentage;

    private String assortment;
    private String articleType;
    private String country;
    private String area;
    private Integer year;
    private String packaging;
    private String producer;
    private String supplier;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    public Double getVolumeInMl() {
        return volumeInMl;
    }

    public void setVolumeInMl(Double volumeInMl) {
        this.volumeInMl = volumeInMl;
    }

    public String getSalesStart() {
        return salesStart;
    }

    public void setSalesStart(String salesStart) {
        this.salesStart = salesStart;
    }

    public Double getAlcoholPercentage() {
        return alcoholPercentage;
    }

    public void setAlcoholPercentage(Double alcoholPercentage) {
        this.alcoholPercentage = alcoholPercentage;
    }

    public String getAssortment() {
        return assortment;
    }

    public void setAssortment(String assortment) {
        this.assortment = assortment;
    }

    public String getArticleType() {
        return articleType;
    }

    public void setArticleType(String articleType) {
        this.articleType = articleType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    @Override
    public String toString() {
        return "ArticleEntity{" +
                "articleId=" + articleId +
                ", name='" + name + '\'' +
                ", name2='" + name2 + '\'' +
                ", price=" + price +
                ", volumeInMl=" + volumeInMl +
                ", salesStart=" + salesStart +
                ", alcoholPercentage=" + alcoholPercentage +
                ", assortment='" + assortment + '\'' +
                ", articleType='" + articleType + '\'' +
                ", country='" + country + '\'' +
                ", year=" + year +
                ", packaging='" + packaging + '\'' +
                ", producer='" + producer + '\'' +
                ", supplier='" + supplier + '\'' +
                '}';
    }
}
