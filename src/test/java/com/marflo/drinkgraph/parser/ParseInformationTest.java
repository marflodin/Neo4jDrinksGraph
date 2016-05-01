package com.marflo.drinkgraph.parser;

import com.marflo.drinkgraph.data.IngredientEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParseInformationTest {
    final String MAIN_PAGE_LOCATION = "pages/main-page.html";
    final String DRINK_PAGE_LOCATION = "pages/drink-page.html";
    final String SLOE_DOWN_URL = "//www.vinguiden.com/cocktailguiden/drinkar/sloe-down/";
    PageParserHelper parserHelper = new PageParserHelper();

    @Test
    public void validateMainPageParsing() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource(MAIN_PAGE_LOCATION).getFile());
        Document doc = Jsoup.parse(input, "UTF-8", "");

        Map<String, String> drinkUrls = parserHelper.parseMainPage(doc);

        assertEquals(18,drinkUrls.size());
        assertTrue(drinkUrls.containsKey("Sloe Down"));
        assertEquals(SLOE_DOWN_URL, drinkUrls.get("Sloe Down"));
    }

    @Test
    public void validateDrinkPageParsing() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource(DRINK_PAGE_LOCATION).getFile());
        Document doc = Jsoup.parse(input, "UTF-8", "");

        List<IngredientEntity> ingredients = parserHelper.parseDrinkPage(doc);
        assertEquals(4, ingredients.size());

        List<String> expectedIngredients = new ArrayList<>();
        expectedIngredients.add("amaretto");
        expectedIngredients.add("citronjuice");
        expectedIngredients.add("sockerlag");
        expectedIngredients.add("äggvita");
        for (IngredientEntity ingredient: ingredients) {
            assertTrue(expectedIngredients.contains(ingredient.getName()));
        }
    }

}
