package com.marflo.drinkgraph.data;

public class PriceClasses {

    private String name;
    private Integer minPrice;
    private Integer maxPrice;

    public PriceClasses(String name, Integer minPrice, Integer maxPrice) {
        this.name = name;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public String getName() {
        return name;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    @Override
    public String toString() {
        return "PriceClasses{" +
                "name='" + name + '\'' +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                '}';
    }
}
