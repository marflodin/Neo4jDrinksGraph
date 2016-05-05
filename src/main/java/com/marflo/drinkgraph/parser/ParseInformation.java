package com.marflo.drinkgraph.parser;

import com.marflo.drinkgraph.data.IngredientEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.neo4j.driver.v1.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;

public class ParseInformation {

    public static void main(String[] args) throws IOException{

        PageParserHelper pageParserHelper = new PageParserHelper();
        Map<String, String> drinkUrls = new HashMap<>();
        Set<String> ingredients = new HashSet<>();
        Map<String, List<IngredientEntity>> drinkIngredientsMapping = new HashMap<>();
        int invalidDrinksCounter = 0;
        int validDrinksCounter = 0;

        for (int pageNumber = 1; pageNumber < 20; pageNumber++) {
            String input = "https://www.vinguiden.com/cocktailguiden/drinkar/?sida=" + pageNumber;
            Document mainDoc = Jsoup.connect(input).get();
            drinkUrls.putAll(pageParserHelper.parseMainPage(mainDoc));
        }
        System.out.println("number of drinks: " + drinkUrls.size());

        for (Map.Entry<String, String> drink : drinkUrls.entrySet()) {
            System.out.println(drink.getKey() + " : " + drink.getValue());
            String drinkInput = "https:" + drink.getValue();
            try {
                Document drinkDoc = Jsoup.connect(drinkInput).get();
                List<IngredientEntity> ingredientsForDrink = pageParserHelper.parseDrinkPage(drinkDoc);
                System.out.println("drink: " + drink.getKey());
                if (ingredientsForDrink != null) {
                    System.out.println("number of ingredients: " + ingredientsForDrink.size());

                    drinkIngredientsMapping.put(drink.getKey(), ingredientsForDrink);
                    for (IngredientEntity ingredient : ingredientsForDrink) {
                        ingredients.add(ingredient.getName());
                    }
                    validDrinksCounter++;
                } else {
                    System.out.println("could not find ingredients for drink: " + invalidDrinksCounter++);
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Connection timed out!");
            }
        }
        System.out.println("number of ingredients: " + ingredients.size());
        System.out.println("Valid drinks: " + validDrinksCounter);
        System.out.println("Invalid drinks: " + invalidDrinksCounter);

        //start neo4j session
        Driver driver = GraphDatabase.driver( "bolt://localhost", AuthTokens.basic( "neo4j", "test123" ) );
        Session session = driver.session();


        //Populate graph
        //Create drink nodes
        for (Map.Entry<String, String> drink : drinkUrls.entrySet()) {
            session.run("MERGE (a:Drink {name: {drinkName}, url:{drinkUrl}})",
                    Values.parameters("drinkName", drink.getKey(), "drinkUrl", drink.getValue()));
        }

        //Create ingredients nodes
        for (String ingredient : ingredients) {
            session.run("MERGE (a:Ingredient {name: {ingredientName}})",
                    Values.parameters("ingredientName", ingredient));
        }

        //Create mappings
        for (Map.Entry<String, List<IngredientEntity>> drinkIngredientConnection : drinkIngredientsMapping.entrySet()) {
            for (IngredientEntity ingredient: drinkIngredientConnection.getValue()) {

                session.run("MATCH (d:Drink) WHERE d.name = {drinkName} " +
                        "MATCH (i:Ingredient) WHERE i.name = {ingredientName} " +
                        "MERGE (d)-[:CONTAINS]->(i)",
                        Values.parameters("drinkName", drinkIngredientConnection.getKey(),
                                "ingredientName", ingredient.getName()));
            }
        }

        //close neo4j session
        session.close();
        driver.close();
    }

}
