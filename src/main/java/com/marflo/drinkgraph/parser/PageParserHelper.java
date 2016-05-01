package com.marflo.drinkgraph.parser;

import com.marflo.drinkgraph.data.IngredientEntity;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class PageParserHelper {

    public Map<String, String> parseMainPage(Document mainPage) {
        Map<String, String> drinkUrls = new HashMap<>();

        Elements drinkWrappers = mainPage.select("li[class=drink-grid-title] > a");
        for (Element drinkWrapper : drinkWrappers) {
            drinkUrls.put(drinkWrapper.attr("title"), drinkWrapper.attr("href"));
        }

        return drinkUrls;
    }

    public List<IngredientEntity> parseDrinkPage(Document drinkPage) {

        Elements drinkContentWrappers = drinkPage.select("div[class=drink-content] > p");
        for (Element drinkContentWrapper: drinkContentWrappers) {
            String drinkContentStr = drinkContentWrapper.toString().toLowerCase();
            if (drinkContentStr.contains("ingredienser:")) {
                return extractIngredientsFromString(drinkContentStr);
            }
        }

        //Find first one with at least 3 line breaks
        for (Element drinkContentWrapper: drinkContentWrappers) {
            String[] split = drinkContentWrapper.toString().trim().split("<br>");
            if (split.length >= 3 && !drinkContentWrapper.toString().toLowerCase().contains("glas:")) {
                return extractIngredientsFromString(drinkContentWrapper.toString().toLowerCase());
            }
        }
        return null;
    }

    private List<IngredientEntity> extractIngredientsFromString(String inputString) {

        inputString = inputString.replaceAll("<p>", "");
        inputString = inputString.replaceAll("</p>", "");
        List<String> drinkIngredients = new ArrayList<>(Arrays.asList(inputString.split("<br>")));
        //remove "header"
        drinkIngredients.remove(0);

        return getIngredientFromString(drinkIngredients);
    }

    private List<IngredientEntity> getIngredientFromString(List<String> ingredientStrings) {
        List<IngredientEntity> ingredients = new ArrayList<>();
        for (String ingredientStr : ingredientStrings) {
            String[] split = ingredientStr.trim().split(" ");
            IngredientEntity ingredientEntity = new IngredientEntity();
            if (split.length == 3) {
                ingredientEntity.setName(split[2]);
            } else if (split.length == 1) {
                ingredientEntity.setName(split[0]);
            } else {
                System.out.println("Unexpected..." + Arrays.toString(split));
                StringBuilder strBuilder = new StringBuilder();
                int start = split.length > 2 ? 2 : 0;
                for (int i = start; i < split.length; i++) {
                    strBuilder.append(split[i] + " ");
                }
                System.out.println("input:" + strBuilder.toString());
                ingredientEntity.setName(strBuilder.toString());
            }
            ingredients.add(ingredientEntity);
        }
        return ingredients;
    }
}
