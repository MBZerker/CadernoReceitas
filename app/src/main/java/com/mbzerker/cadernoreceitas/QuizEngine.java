package com.mbzerker.cadernoreceitas;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class QuizEngine {
    static final int MODEL_COUNT = 100;
    static final int ROUND_SIZE = 30;

    private QuizEngine() {}

    static ArrayList<Question> buildRound(List<RecipeData> recipes) {
        ContextData ctx = new ContextData(recipes);
        ArrayList<Question> round = new ArrayList<>();
        LinkedHashSet<Integer> usedModels = new LinkedHashSet<>();
        LinkedHashSet<String> usedPrompts = new LinkedHashSet<>();
        Random random = new Random();
        for (int slot = 0; slot < ROUND_SIZE; slot++) {
            Question question = pickQuestion(ctx, levelsForSlot(slot), usedModels, usedPrompts, random);
            if (question == null) question = pickQuestion(ctx, new int[]{1, 2, 3, 4, 5}, usedModels, usedPrompts, random);
            if (question == null) break;
            round.add(question);
        }
        return round;
    }

    private static int[] levelsForSlot(int slot) {
        if (slot < 6) return new int[]{1};
        if (slot < 12) return new int[]{1, 2};
        if (slot < 18) return new int[]{2, 3};
        if (slot < 24) return new int[]{3, 4};
        return new int[]{4, 5};
    }

    private static Question pickQuestion(ContextData ctx, int[] levels, LinkedHashSet<Integer> usedModels, LinkedHashSet<String> usedPrompts, Random random) {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int id = 1; id <= MODEL_COUNT; id++) {
            if (usedModels.contains(id)) continue;
            if (contains(levels, levelFor(id))) ids.add(id);
        }
        Collections.shuffle(ids, random);
        for (Integer id : ids) {
            Question question = buildModel(ctx, id, random);
            if (question == null) continue;
            String key = norm(question.prompt);
            if (usedPrompts.contains(key)) continue;
            usedModels.add(id);
            usedPrompts.add(key);
            return question;
        }
        return null;
    }

    private static int levelFor(int id) {
        if (id <= 20) return 1;
        if (id <= 40) return 2;
        if (id <= 60) return 3;
        if (id <= 80) return 4;
        return 5;
    }

    private static boolean contains(int[] values, int wanted) {
        for (int value : values) if (value == wanted) return true;
        return false;
    }

    private static Question buildModel(ContextData c, int id, Random r) {
        int level = levelFor(id);
        switch (id) {
            case 1: return ingredientBelongs(c, id, level, r);
            case 2: return ingredientDoesNotBelong(c, id, level, r);
            case 3: return quantityForIngredient(c, id, level, r);
            case 4: return ingredientForQuantity(c, id, level, r);
            case 5: return quantityIngredientPair(c, id, level, r);
            case 6: return categoryForIngredient(c, id, level, r);
            case 7: return ingredientForCategory(c, id, level, r);
            case 8: return categoryIngredientPair(c, id, level, r);
            case 9: return aGostoIngredient(c, id, level, r);
            case 10: return unitForIngredient(c, id, level, r);
            case 11: return ingredientUnitPair(c, id, level, r);
            case 12: return ingredientCount(c, id, level, r);
            case 13: return linkedIngredient(c, id, level, r);
            case 14: return linkedRecipeTarget(c, id, level, r);
            case 15: return rawIngredient(c, id, level, r);
            case 16: return baseIngredient(c, id, level, r);
            case 17: return complementaryIngredient(c, id, level, r);
            case 18: return realIngredientPair(c, id, level, r);
            case 19: return mixedPairIntruder(c, id, level, r);
            case 20: return fullQuantityUnitCombo(c, id, level, r);
            case 21: return ingredientOnlyInFirst(c, id, level, r);
            case 22: return ingredientOnlyInSecond(c, id, level, r);
            case 23: return commonIngredient(c, id, level, r);
            case 24: return recipeByTwoIngredients(c, id, level, r);
            case 25: return recipeWithMoreIngredients(c, id, level, r);
            case 26: return recipeWithFewerIngredients(c, id, level, r);
            case 27: return recipeWithSameIngredientCount(c, id, level, r);
            case 28: return recipeWhereIngredientHasMoreQuantity(c, id, level, r);
            case 29: return recipeWhereIngredientHasLessQuantity(c, id, level, r);
            case 30: return recipeWithMostCategory(c, id, level, r);
            case 31: return recipeWithSingleCategory(c, id, level, r);
            case 32: return recipePairSharesIngredient(c, id, level, r);
            case 33: return recipePairDoesNotShareIngredient(c, id, level, r);
            case 34: return recipeContainsLinkedRecipe(c, id, level, r);
            case 35: return recipeDoesNotContainLinkedRecipe(c, id, level, r);
            case 36: return recipeByTwoIngredients(c, id, level, r);
            case 37: return ingredientInAllRecipes(c, id, level, r);
            case 38: return ingredientInOnlyOneComparedRecipe(c, id, level, r);
            case 39: return simplestRecipe(c, id, level, r);
            case 40: return recipeWithTwoWithoutThird(c, id, level, r);
            case 41: return prepSnippetBelongs(c, id, level, r);
            case 42: return recipeByPrepSnippet(c, id, level, r);
            case 43: return firstPrepStep(c, id, level, r);
            case 44: return stepAfterSpecificStep(c, id, level, r);
            case 45: return stepBeforeSpecificStep(c, id, level, r);
            case 46: return ingredientInPrepStep(c, id, level, r);
            case 47: return stepAfterIngredient(c, id, level, r);
            case 48: return actionBeforeFinalStep(c, id, level, r);
            case 49: return ingredientsTogetherInStep(c, id, level, r);
            case 50: return finalPrepStep(c, id, level, r);
            case 51: return correctPrepSequence(c, id, level, r);
            case 52: return prepStepIntruder(c, id, level, r);
            case 53: return ingredientInSheetAndPrep(c, id, level, r);
            case 54: return ingredientMissingFromPrep(c, id, level, r);
            case 55: return stepBeforeServing(c, id, level, r);
            case 56: return ingredientsTogetherInStep(c, id, level, r);
            case 57: return ingredientCompletesStep(c, id, level, r);
            case 58: return heatingStep(c, id, level, r);
            case 59: return mixingStep(c, id, level, r);
            case 60: return stepPosition(c, id, level, r);
            case 61: return linkedIngredient(c, id, level, r);
            case 62: return linkedRecipeBeforeMain(c, id, level, r);
            case 63: return linkedRecipeTarget(c, id, level, r);
            case 64: return linkedRecipeUsedMoreThanOnce(c, id, level, r);
            case 65: return recipeWithMoreThanOneLinked(c, id, level, r);
            case 66: return recipeOnlyRawIngredients(c, id, level, r);
            case 67: return linkedIngredientNotRaw(c, id, level, r);
            case 68: return linkedAndRawCombo(c, id, level, r);
            case 69: return linkedRecipeIngredientPair(c, id, level, r);
            case 70: return categoryWithLinkedIngredient(c, id, level, r);
            case 71: return fullLinkedCombo(c, id, level, r);
            case 72: return recipeWithCategoryAndLink(c, id, level, r);
            case 73: return ingredientWithCategoryAndFeature(c, id, level, r);
            case 74: return recipeWithGramAndLiter(c, id, level, r);
            case 75: return ingredientByUnit(c, id, level, r, "g");
            case 76: return ingredientByUnit(c, id, level, r, "L");
            case 77: return unitForIngredient(c, id, level, r);
            case 78: return ingredientCategoryUnitCombo(c, id, level, r);
            case 79: return wrongCategoryForIngredient(c, id, level, r);
            case 80: return sharedIngredient(c, id, level, r);
            case 81: return correctIngredientWrongQuantity(c, id, level, r);
            case 82: return correctQuantityWrongIngredient(c, id, level, r);
            case 83: return correctIngredientWrongUnit(c, id, level, r);
            case 84: return correctCategoryIntruderIngredient(c, id, level, r);
            case 85: return similarIngredientName(c, id, level, r);
            case 86: return confusedIngredientBetweenRecipes(c, id, level, r);
            case 87: return mixedPairIntruder(c, id, level, r);
            case 88: return recipeWithTwoWithoutThird(c, id, level, r);
            case 89: return commonIngredientDifferentQuantity(c, id, level, r);
            case 90: return ingredientMissingFromPrep(c, id, level, r);
            case 91: return finalPrepStep(c, id, level, r);
            case 92: return threeStepSequence(c, id, level, r);
            case 93: return fullIngredientCategoryUnitCombo(c, id, level, r);
            case 94: return comboWithOneWrongField(c, id, level, r);
            case 95: return aGostoIngredient(c, id, level, r);
            case 96: return aGostoIngredient(c, id, level, r);
            case 97: return ingredientInPrepStep(c, id, level, r);
            case 98: return onlyRecipeIngredientsOption(c, id, level, r);
            case 99: return correctAndOtherRecipeIngredient(c, id, level, r);
            case 100: return twoSnippetsToTwoRecipes(c, id, level, r);
            default: return null;
        }
    }

    private static Question ingredientBelongs(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 1, r);
        if (recipe == null) return null;
        IngredientData correct = any(recipe.ingredients, r);
        return question(id, level, "Qual ingrediente pertence a \"" + recipe.name + "\"?", correct.name, ingredientDistractors(c, recipe, correct, 3));
    }

    private static Question ingredientDoesNotBelong(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 2, r);
        if (recipe == null) return null;
        IngredientData intruder = outsiderIngredient(c, recipe, null, r);
        if (intruder == null) return null;
        return question(id, level, "Qual ingrediente nao pertence a \"" + recipe.name + "\"?", intruder.name, ingredientNames(recipe.ingredients, intruder.name, 3));
    }

    private static Question quantityForIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasQuantity(), r);
        if (pick == null) return null;
        return question(id, level, "Na receita \"" + pick.recipe.name + "\", qual quantidade acompanha \"" + pick.ingredient.name + "\"?", pick.ingredient.quantity, quantityDistractors(c, pick.ingredient.quantity, 3));
    }

    private static Question ingredientForQuantity(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasQuantity(), r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente de \"" + pick.recipe.name + "\" esta cadastrado com \"" + pick.ingredient.quantity + "\"?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question quantityIngredientPair(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasQuantity(), r);
        if (pick == null) return null;
        return question(id, level, "Qual par quantidade + ingrediente pertence a \"" + pick.recipe.name + "\"?", quantityIngredient(pick.ingredient), comboDistractors(quantityIngredient(pick.ingredient), quantityIngredientOptions(c, pick.recipe), 3));
    }

    private static Question categoryForIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasCategory(), r);
        if (pick == null) return null;
        return question(id, level, "Em \"" + pick.recipe.name + "\", qual categoria organiza \"" + pick.ingredient.name + "\"?", pick.ingredient.category, categoryDistractors(c, pick.ingredient.category, 3));
    }

    private static Question ingredientForCategory(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWith(c, recipeData -> items(recipeData, i -> i.hasCategory()).size() >= 2, r);
        if (recipe == null) return null;
        IngredientData correct = any(items(recipe, i -> i.hasCategory()), r);
        ArrayList<String> pool = ingredientNames(items(recipe, i -> norm(i.category).equals(norm(correct.category))), correct.name, 3);
        pool.addAll(ingredientDistractors(c, recipe, correct, 3));
        return question(id, level, "Qual ingrediente de \"" + recipe.name + "\" esta dentro da categoria \"" + correct.category + "\"?", correct.name, pool);
    }

    private static Question categoryIngredientPair(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasCategory(), r);
        if (pick == null) return null;
        return question(id, level, "Qual par categoria + ingrediente pertence a \"" + pick.recipe.name + "\"?", categoryIngredient(pick.ingredient), comboDistractors(categoryIngredient(pick.ingredient), categoryIngredientOptions(c, pick.recipe), 3));
    }

    private static Question aGostoIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, IngredientData::isAGosto, r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente de \"" + pick.recipe.name + "\" esta marcado como \"a gosto\"?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question unitForIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasMeasuredUnit(), r);
        if (pick == null) return null;
        return question(id, level, "Qual unidade acompanha \"" + pick.ingredient.name + "\" em \"" + pick.recipe.name + "\"?", pick.ingredient.unit(), unitDistractors(pick.ingredient.unit(), 3));
    }

    private static Question ingredientUnitPair(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasMeasuredUnit(), r);
        if (pick == null) return null;
        return question(id, level, "Qual par ingrediente + unidade pertence a \"" + pick.recipe.name + "\"?", ingredientUnit(pick.ingredient), comboDistractors(ingredientUnit(pick.ingredient), ingredientUnitOptions(c, pick.recipe), 3));
    }

    private static Question ingredientCount(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 1, r);
        if (recipe == null) return null;
        int count = recipe.ingredients.size();
        return question(id, level, "Quantos ingredientes estao cadastrados em \"" + recipe.name + "\"?", countLabel(count), numberDistractors(count, 3));
    }

    private static Question linkedIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.isLinked(), r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente de \"" + pick.recipe.name + "\" tambem e uma receita preparada?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question linkedRecipeTarget(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.isLinked() && !i.linkedRecipeName.isEmpty(), r);
        if (pick == null) return null;
        return question(id, level, "O ingrediente \"" + pick.ingredient.name + "\" aponta para qual receita preparada?", pick.ingredient.linkedRecipeName, recipeDistractors(c, pick.ingredient.linkedRecipeName, 3));
    }

    private static Question rawIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> !i.isLinked(), r);
        if (pick == null) return null;
        return question(id, level, "Qual item de \"" + pick.recipe.name + "\" e ingrediente comum, sem receita vinculada?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question baseIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> hasCategoryWord(i, "base") || hasCategoryWord(i, "principal") || hasCategoryWord(i, "proteina"), r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente faz parte da base principal de \"" + pick.recipe.name + "\"?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question complementaryIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasCategory() && !hasCategoryWord(i, "base") && !hasCategoryWord(i, "principal"), r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente complementar tambem faz parte de \"" + pick.recipe.name + "\"?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question realIngredientPair(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 2, r);
        if (recipe == null) return null;
        String correct = pairNames(twoIngredients(recipe, r));
        return question(id, level, "Qual dupla de ingredientes pertence a \"" + recipe.name + "\"?", correct, pairDistractors(c, recipe, correct, 3));
    }

    private static Question mixedPairIntruder(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 1, r);
        if (recipe == null) return null;
        IngredientData outsider = outsiderIngredient(c, recipe, null, r);
        if (outsider == null) return null;
        IngredientData inside = any(recipe.ingredients, r);
        String correct = inside.name + " + " + outsider.name;
        return question(id, level, "Qual dupla tem um ingrediente correto de \"" + recipe.name + "\" e um intruso?", correct, pairDistractors(c, recipe, correct, 3));
    }

    private static Question fullQuantityUnitCombo(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasQuantity() && i.hasMeasuredUnit(), r);
        if (pick == null) return null;
        return question(id, level, "Qual alternativa representa corretamente ingrediente + quantidade + unidade em \"" + pick.recipe.name + "\"?", ingredientQuantityUnit(pick.ingredient), comboDistractors(ingredientQuantityUnit(pick.ingredient), ingredientQuantityUnitOptions(c, pick.recipe), 3));
    }

    private static Question ingredientOnlyInFirst(ContextData c, int id, int level, Random r) {
        RecipePair pair = pairWithDifference(c, r);
        if (pair == null || pair.onlyA.isEmpty()) return null;
        IngredientData correct = any(pair.onlyA, r);
        return question(id, level, "Qual ingrediente aparece em \"" + pair.a.name + "\", mas nao em \"" + pair.b.name + "\"?", correct.name, namesFrom(pair.onlyB, c.allIngredientNames, correct.name, 3));
    }

    private static Question ingredientOnlyInSecond(ContextData c, int id, int level, Random r) {
        RecipePair pair = pairWithDifference(c, r);
        if (pair == null || pair.onlyB.isEmpty()) return null;
        IngredientData correct = any(pair.onlyB, r);
        return question(id, level, "Qual ingrediente aparece em \"" + pair.b.name + "\", mas nao em \"" + pair.a.name + "\"?", correct.name, namesFrom(pair.onlyA, c.allIngredientNames, correct.name, 3));
    }

    private static Question commonIngredient(ContextData c, int id, int level, Random r) {
        RecipePair pair = pairWithCommon(c, r);
        if (pair == null) return null;
        IngredientData correct = any(pair.common, r);
        return question(id, level, "Qual ingrediente aparece tanto em \"" + pair.a.name + "\" quanto em \"" + pair.b.name + "\"?", correct.name, namesFrom(pair.onlyA, c.allIngredientNames, correct.name, 3));
    }

    private static Question recipeByTwoIngredients(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 2, r);
        if (recipe == null) return null;
        List<IngredientData> pair = twoIngredients(recipe, r);
        return question(id, level, "Qual receita usa \"" + pair.get(0).name + "\" e \"" + pair.get(1).name + "\"?", recipe.name, recipeDistractors(c, recipe.name, 3));
    }

    private static Question recipeWithMoreIngredients(ContextData c, int id, int level, Random r) {
        return recipeByCount(c, id, level, r, true, "Qual receita tem mais ingredientes cadastrados entre estas opcoes?");
    }

    private static Question recipeWithFewerIngredients(ContextData c, int id, int level, Random r) {
        return recipeByCount(c, id, level, r, false, "Qual receita tem menos ingredientes cadastrados entre estas opcoes?");
    }

    private static Question recipeWithSameIngredientCount(ContextData c, int id, int level, Random r) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            for (RecipeData other : c.recipes) {
                if (recipe.id != other.id && recipe.ingredients.size() == other.ingredients.size()) {
                    return question(id, level, "Qual receita tem a mesma quantidade de ingredientes que \"" + recipe.name + "\"?", other.name, recipesWithDifferentCount(c, recipe.ingredients.size(), other.name, 3));
                }
            }
        }
        return null;
    }

    private static Question recipeWhereIngredientHasMoreQuantity(ContextData c, int id, int level, Random r) {
        return recipeWhereIngredientQuantityExtreme(c, id, level, r, true);
    }

    private static Question recipeWhereIngredientHasLessQuantity(ContextData c, int id, int level, Random r) {
        return recipeWhereIngredientQuantityExtreme(c, id, level, r, false);
    }

    private static Question recipeWithMostCategory(ContextData c, int id, int level, Random r) {
        String category = any(c.allCategories, r);
        if (category == null) return null;
        RecipeData best = null;
        int bestCount = 1;
        for (RecipeData recipe : c.recipes) {
            int count = countCategory(recipe, category);
            if (count > bestCount) {
                best = recipe;
                bestCount = count;
            }
        }
        if (best == null) return null;
        return question(id, level, "Qual receita possui mais ingredientes da categoria \"" + category + "\"?", best.name, recipeDistractors(c, best.name, 3));
    }

    private static Question recipeWithSingleCategory(ContextData c, int id, int level, Random r) {
        for (String category : shuffled(c.allCategories, r)) {
            for (RecipeData recipe : shuffled(c.recipes, r)) {
                if (countCategory(recipe, category) == 1) {
                    return question(id, level, "Qual receita possui apenas um ingrediente da categoria \"" + category + "\"?", recipe.name, recipesByCategoryCount(c, category, 1, recipe.name, 3));
                }
            }
        }
        return null;
    }

    private static Question recipePairSharesIngredient(ContextData c, int id, int level, Random r) {
        RecipePair pair = pairWithCommon(c, r);
        if (pair == null) return null;
        IngredientData shared = any(pair.common, r);
        String correct = pair.a.name + " + " + pair.b.name;
        return question(id, level, "Qual par de receitas compartilha \"" + shared.name + "\"?", correct, recipePairDistractors(c, correct, 3));
    }

    private static Question recipePairDoesNotShareIngredient(ContextData c, int id, int level, Random r) {
        RecipePair pair = pairWithoutCommon(c, r);
        if (pair == null) return null;
        IngredientData ingredient = any(pair.a.ingredients, r);
        String correct = pair.a.name + " + " + pair.b.name;
        return question(id, level, "Qual par de receitas nao compartilha \"" + ingredient.name + "\"?", correct, recipePairDistractors(c, correct, 3));
    }

    private static Question recipeContainsLinkedRecipe(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.isLinked() && !i.linkedRecipeName.isEmpty(), r);
        if (pick == null) return null;
        return question(id, level, "Qual receita contem a receita preparada \"" + pick.ingredient.linkedRecipeName + "\"?", pick.recipe.name, recipeDistractors(c, pick.recipe.name, 3));
    }

    private static Question recipeDoesNotContainLinkedRecipe(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.isLinked() && !i.linkedRecipeName.isEmpty(), r);
        if (pick == null) return null;
        ArrayList<String> containing = new ArrayList<>();
        for (RecipeData recipe : c.recipes) {
            if (recipeContainsLinked(recipe, pick.ingredient.linkedRecipeId)) addUnique(containing, recipe.name);
        }
        ArrayList<String> notContaining = without(c.recipeNames, containing);
        String correct = any(notContaining, r);
        if (correct == null) return null;
        return question(id, level, "Qual receita nao contem a receita preparada \"" + pick.ingredient.linkedRecipeName + "\"?", correct, containing);
    }

    private static Question ingredientInAllRecipes(ContextData c, int id, int level, Random r) {
        if (c.recipes.size() < 3) return null;
        for (String ingredient : shuffled(c.allIngredientNames, r)) {
            int count = 0;
            for (RecipeData recipe : c.recipes) if (recipeHasIngredient(recipe, ingredient)) count++;
            if (count >= 3) {
                return question(id, level, "Qual ingrediente aparece em pelo menos tres receitas deste caderno?", ingredient, ingredientNameDistractors(c, ingredient, 3));
            }
        }
        return null;
    }

    private static Question ingredientInOnlyOneComparedRecipe(ContextData c, int id, int level, Random r) {
        for (String ingredient : shuffled(c.allIngredientNames, r)) {
            int count = 0;
            RecipeData owner = null;
            for (RecipeData recipe : c.recipes) {
                if (recipeHasIngredient(recipe, ingredient)) {
                    count++;
                    owner = recipe;
                }
            }
            if (count == 1 && owner != null) {
                return question(id, level, "Qual ingrediente aparece em apenas uma receita deste caderno?", ingredient, ingredientNameDistractors(c, ingredient, 3));
            }
        }
        return null;
    }

    private static Question simplestRecipe(ContextData c, int id, int level, Random r) {
        return recipeByCount(c, id, level, r, false, "Qual receita e mais simples considerando a quantidade de ingredientes?");
    }

    private static Question recipeWithTwoWithoutThird(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 2, r);
        if (recipe == null) return null;
        IngredientData outside = outsiderIngredient(c, recipe, null, r);
        if (outside == null) return null;
        List<IngredientData> pair = twoIngredients(recipe, r);
        return question(id, level, "Qual receita contem \"" + pair.get(0).name + "\" + \"" + pair.get(1).name + "\", mas nao contem \"" + outside.name + "\"?", recipe.name, recipeDistractors(c, recipe.name, 3));
    }

    private static Question prepSnippetBelongs(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 1, r);
        if (recipe == null) return null;
        String correct = prepSnippet(recipe.prep);
        return question(id, level, "Qual trecho de preparo pertence a \"" + recipe.name + "\"?", correct, prepSnippetDistractors(c, recipe, correct, 3));
    }

    private static Question recipeByPrepSnippet(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 1, r);
        if (recipe == null) return null;
        return question(id, level, "Dado o trecho \"" + prepSnippet(recipe.prep) + "\", qual e a receita?", recipe.name, recipeDistractors(c, recipe.name, 3));
    }

    private static Question firstPrepStep(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 2, r);
        if (recipe == null) return null;
        return question(id, level, "Em \"" + recipe.name + "\", qual etapa acontece primeiro?", recipe.steps.get(0), stepDistractors(c, recipe, recipe.steps.get(0), 3));
    }

    private static Question stepAfterSpecificStep(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 3, r);
        if (recipe == null) return null;
        int index = r.nextInt(recipe.steps.size() - 1);
        String current = recipe.steps.get(index);
        String correct = recipe.steps.get(index + 1);
        return question(id, level, "Depois de \"" + shortText(current) + "\", o que acontece em \"" + recipe.name + "\"?", correct, stepDistractors(c, recipe, correct, 3));
    }

    private static Question stepBeforeSpecificStep(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 3, r);
        if (recipe == null) return null;
        int index = 1 + r.nextInt(recipe.steps.size() - 1);
        String current = recipe.steps.get(index);
        String correct = recipe.steps.get(index - 1);
        return question(id, level, "Antes de \"" + shortText(current) + "\", qual etapa vem em \"" + recipe.name + "\"?", correct, stepDistractors(c, recipe, correct, 3));
    }

    private static Question ingredientInPrepStep(ContextData c, int id, int level, Random r) {
        PrepIngredient prep = prepIngredient(c, r);
        if (prep == null) return null;
        return question(id, level, "Qual ingrediente e usado nesta etapa de \"" + prep.recipe.name + "\": \"" + shortText(prep.step) + "\"?", prep.ingredient.name, ingredientDistractors(c, prep.recipe, prep.ingredient, 3));
    }

    private static Question stepAfterIngredient(ContextData c, int id, int level, Random r) {
        PrepIngredient prep = prepIngredient(c, r);
        if (prep == null) return null;
        int index = prep.recipe.steps.indexOf(prep.step);
        if (index < 0 || index >= prep.recipe.steps.size() - 1) return null;
        String correct = prep.recipe.steps.get(index + 1);
        return question(id, level, "Depois de usar \"" + prep.ingredient.name + "\" em \"" + prep.recipe.name + "\", o que acontece em seguida?", correct, stepDistractors(c, prep.recipe, correct, 3));
    }

    private static Question actionBeforeFinalStep(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 3, r);
        if (recipe == null) return null;
        String correct = recipe.steps.get(recipe.steps.size() - 2);
        return question(id, level, "Antes da etapa final de \"" + recipe.name + "\", qual acao acontece?", correct, stepDistractors(c, recipe, correct, 3));
    }

    private static Question ingredientsTogetherInStep(ContextData c, int id, int level, Random r) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            for (String step : recipe.steps) {
                ArrayList<IngredientData> mentioned = ingredientsInStep(recipe, step);
                if (mentioned.size() >= 2) {
                    String correct = mentioned.get(0).name + " + " + mentioned.get(1).name;
                    return question(id, level, "Qual dupla aparece junta nesta etapa de \"" + recipe.name + "\": \"" + shortText(step) + "\"?", correct, pairDistractors(c, recipe, correct, 3));
                }
            }
        }
        return null;
    }

    private static Question finalPrepStep(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 2, r);
        if (recipe == null) return null;
        String correct = recipe.steps.get(recipe.steps.size() - 1);
        return question(id, level, "Qual etapa de finalizacao pertence a \"" + recipe.name + "\"?", correct, stepDistractors(c, recipe, correct, 3));
    }

    private static Question correctPrepSequence(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 3, r);
        if (recipe == null) return null;
        String correct = sequence(recipe.steps, 0, Math.min(3, recipe.steps.size()));
        return question(id, level, "Qual sequencia de preparo esta correta em \"" + recipe.name + "\"?", correct, sequenceDistractors(recipe, correct, 3, r));
    }

    private static Question prepStepIntruder(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 2, r);
        if (recipe == null) return null;
        String correct = stepFromOtherRecipe(c, recipe, r);
        if (correct == null) return null;
        return question(id, level, "Qual etapa nao pertence ao preparo de \"" + recipe.name + "\"?", correct, stepOptions(recipe.steps, correct, 3));
    }

    private static Question ingredientInSheetAndPrep(ContextData c, int id, int level, Random r) {
        PrepIngredient prep = prepIngredient(c, r);
        if (prep == null) return null;
        return question(id, level, "Qual ingrediente aparece tanto na ficha quanto no preparo de \"" + prep.recipe.name + "\"?", prep.ingredient.name, ingredientDistractors(c, prep.recipe, prep.ingredient, 3));
    }

    private static Question ingredientMissingFromPrep(ContextData c, int id, int level, Random r) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            if (recipe.steps.isEmpty()) continue;
            ArrayList<IngredientData> quiet = new ArrayList<>();
            for (IngredientData ingredient : recipe.ingredients) {
                if (!norm(recipe.prep).contains(norm(ingredient.name))) quiet.add(ingredient);
            }
            if (!quiet.isEmpty()) {
                IngredientData correct = any(quiet, r);
                return question(id, level, "Qual ingrediente esta cadastrado em \"" + recipe.name + "\", mas nao aparece literalmente no preparo?", correct.name, ingredientDistractors(c, recipe, correct, 3));
            }
        }
        return null;
    }

    private static Question stepBeforeServing(ContextData c, int id, int level, Random r) {
        return actionBeforeFinalStep(c, id, level, r);
    }

    private static Question ingredientCompletesStep(ContextData c, int id, int level, Random r) {
        PrepIngredient prep = prepIngredient(c, r);
        if (prep == null) return null;
        String blanked = replaceIgnoreCase(prep.step, prep.ingredient.name, "_____");
        if (blanked.equals(prep.step)) return null;
        return question(id, level, "Qual ingrediente completa a etapa: \"" + shortText(blanked) + "\"?", prep.ingredient.name, ingredientDistractors(c, prep.recipe, prep.ingredient, 3));
    }

    private static Question heatingStep(ContextData c, int id, int level, Random r) {
        return stepByWords(c, id, level, r, new String[]{"cozin", "assar", "ferv", "aquec", "frig", "dour", "forno"}, "Qual etapa representa cozimento ou aquecimento?");
    }

    private static Question mixingStep(ContextData c, int id, int level, Random r) {
        return stepByWords(c, id, level, r, new String[]{"mistur", "mex", "bater", "incorpor", "mont", "junt"}, "Qual etapa representa mistura ou montagem?");
    }

    private static Question stepPosition(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 3, r);
        if (recipe == null) return null;
        int pos = r.nextInt(3);
        int index = pos == 0 ? 0 : (pos == 1 ? recipe.steps.size() / 2 : recipe.steps.size() - 1);
        String label = pos == 0 ? "comeco" : (pos == 1 ? "meio" : "final");
        String correct = recipe.steps.get(index);
        return question(id, level, "Qual etapa ocorre no " + label + " do preparo de \"" + recipe.name + "\"?", correct, stepDistractors(c, recipe, correct, 3));
    }

    private static Question linkedRecipeBeforeMain(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.isLinked() && !i.linkedRecipeName.isEmpty(), r);
        if (pick == null) return null;
        return question(id, level, "Qual receita preparada precisa estar pronta antes de \"" + pick.recipe.name + "\"?", pick.ingredient.linkedRecipeName, recipeDistractors(c, pick.ingredient.linkedRecipeName, 3));
    }

    private static Question linkedRecipeUsedMoreThanOnce(ContextData c, int id, int level, Random r) {
        HashMap<Integer, Integer> counts = new HashMap<>();
        HashMap<Integer, String> names = new HashMap<>();
        for (IngredientData ingredient : c.allIngredients) {
            if (ingredient.isLinked()) {
                counts.put(ingredient.linkedRecipeId, counts.containsKey(ingredient.linkedRecipeId) ? counts.get(ingredient.linkedRecipeId) + 1 : 1);
                names.put(ingredient.linkedRecipeId, ingredient.linkedRecipeName);
            }
        }
        for (Integer recipeId : counts.keySet()) {
            if (counts.get(recipeId) > 1) {
                return question(id, level, "Qual receita preparada e usada como ingrediente em mais de uma receita?", names.get(recipeId), recipeDistractors(c, names.get(recipeId), 3));
            }
        }
        return null;
    }

    private static Question recipeWithMoreThanOneLinked(ContextData c, int id, int level, Random r) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            if (items(recipe, IngredientData::isLinked).size() > 1) {
                return question(id, level, "Qual receita utiliza mais de uma receita preparada vinculada?", recipe.name, recipeDistractors(c, recipe.name, 3));
            }
        }
        return null;
    }

    private static Question recipeOnlyRawIngredients(ContextData c, int id, int level, Random r) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            if (!recipe.ingredients.isEmpty() && items(recipe, IngredientData::isLinked).isEmpty()) {
                return question(id, level, "Qual receita utiliza apenas ingredientes comuns, sem receitas vinculadas?", recipe.name, recipeDistractors(c, recipe.name, 3));
            }
        }
        return null;
    }

    private static Question linkedIngredientNotRaw(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, IngredientData::isLinked, r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente nao e materia-prima simples, mas sim uma receita preparada?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question linkedAndRawCombo(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWith(c, x -> !items(x, IngredientData::isLinked).isEmpty() && !items(x, i -> !i.isLinked()).isEmpty(), r);
        if (recipe == null) return null;
        IngredientData linked = any(items(recipe, IngredientData::isLinked), r);
        IngredientData raw = any(items(recipe, i -> !i.isLinked()), r);
        String correct = linked.name + " + " + raw.name;
        return question(id, level, "Qual alternativa mistura uma receita vinculada correta com um ingrediente comum correto de \"" + recipe.name + "\"?", correct, pairDistractors(c, recipe, correct, 3));
    }

    private static Question linkedRecipeIngredientPair(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWith(c, x -> !items(x, IngredientData::isLinked).isEmpty() && x.ingredients.size() >= 2, r);
        if (recipe == null) return null;
        IngredientData linked = any(items(recipe, IngredientData::isLinked), r);
        IngredientData other = any(items(recipe, i -> i.id != linked.id), r);
        String correct = linked.linkedRecipeName + " + " + other.name;
        return question(id, level, "Qual par receita vinculada + ingrediente pertence a \"" + recipe.name + "\"?", correct, comboDistractors(correct, linkedRecipeIngredientOptions(c, recipe), 3));
    }

    private static Question categoryWithLinkedIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.isLinked() && i.hasCategory(), r);
        if (pick == null) return null;
        return question(id, level, "Qual categoria contem um ingrediente que tambem e uma receita em \"" + pick.recipe.name + "\"?", pick.ingredient.category, categoryDistractors(c, pick.ingredient.category, 3));
    }

    private static Question fullLinkedCombo(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.isLinked() && i.hasQuantity() && i.hasMeasuredUnit(), r);
        if (pick == null) return null;
        String correct = ingredientQuantityUnit(pick.ingredient) + " -> " + pick.ingredient.linkedRecipeName;
        return question(id, level, "Qual alternativa representa corretamente ingrediente + quantidade + unidade + vinculo em \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, linkedFullOptions(c, pick.recipe), 3));
    }

    private static Question recipeWithCategoryAndLink(ContextData c, int id, int level, Random r) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            IngredientData categoryItem = any(items(recipe, IngredientData::hasCategory), r);
            if (categoryItem != null && !items(recipe, IngredientData::isLinked).isEmpty()) {
                return question(id, level, "Qual receita contem um ingrediente da categoria \"" + categoryItem.category + "\" e tambem uma receita vinculada?", recipe.name, recipeDistractors(c, recipe.name, 3));
            }
        }
        return null;
    }

    private static Question ingredientWithCategoryAndFeature(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasCategory() && (i.hasQuantity() || i.hasMeasuredUnit() || i.isAGosto()), r);
        if (pick == null) return null;
        String feature = pick.ingredient.isAGosto() ? "a gosto" : (pick.ingredient.hasMeasuredUnit() ? pick.ingredient.unit() : pick.ingredient.quantity);
        return question(id, level, "Qual ingrediente pertence a categoria \"" + pick.ingredient.category + "\" e tambem esta marcado como \"" + feature + "\"?", pick.ingredient.name, ingredientDistractors(c, pick.recipe, pick.ingredient, 3));
    }

    private static Question recipeWithGramAndLiter(ContextData c, int id, int level, Random r) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            if (recipeHasUnit(recipe, "g") && recipeHasUnit(recipe, "L")) {
                return question(id, level, "Qual receita contem ingredientes medidos em gramas e tambem em litro?", recipe.name, recipeDistractors(c, recipe.name, 3));
            }
        }
        return null;
    }

    private static Question ingredientByUnit(ContextData c, int id, int level, Random r, String unit) {
        IngredientPick pick = pickIngredient(c, i -> norm(i.unit()).equals(norm(unit)), r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente esta medido em \"" + unit + "\"?", pick.ingredient.name, ingredientNameDistractors(c, pick.ingredient.name, 3));
    }

    private static Question ingredientCategoryUnitCombo(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasCategory() && i.hasMeasuredUnit(), r);
        if (pick == null) return null;
        String correct = pick.ingredient.name + " - " + pick.ingredient.category + " - " + pick.ingredient.unit();
        return question(id, level, "Qual alternativa contem ingrediente + categoria + unidade corretamente em \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, ingredientCategoryUnitOptions(c, pick.recipe), 3));
    }

    private static Question wrongCategoryForIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasCategory(), r);
        if (pick == null) return null;
        String wrong = any(without(c.allCategories, listOf(pick.ingredient.category)), r);
        if (wrong == null) return null;
        String correct = pick.ingredient.name + " - " + wrong;
        return question(id, level, "Qual alternativa apresenta categoria errada para um ingrediente valido de \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, categoryIngredientOptions(c, pick.recipe), 3));
    }

    private static Question sharedIngredient(ContextData c, int id, int level, Random r) {
        for (String ingredient : shuffled(c.allIngredientNames, r)) {
            int count = 0;
            for (RecipeData recipe : c.recipes) if (recipeHasIngredient(recipe, ingredient)) count++;
            if (count > 1) return question(id, level, "Qual ingrediente e compartilhado por mais de uma receita?", ingredient, ingredientNameDistractors(c, ingredient, 3));
        }
        return null;
    }

    private static Question correctIngredientWrongQuantity(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasQuantity() && !i.isAGosto(), r);
        if (pick == null) return null;
        String wrongQuantity = nearbyQuantity(pick.ingredient);
        if (wrongQuantity.isEmpty()) return null;
        String correct = pick.ingredient.name + " - " + wrongQuantity;
        return question(id, level, "Qual alternativa traz ingrediente correto de \"" + pick.recipe.name + "\", mas com quantidade errada?", correct, comboDistractors(correct, ingredientQuantityOptions(c, pick.recipe), 3));
    }

    private static Question correctQuantityWrongIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, IngredientData::hasQuantity, r);
        if (pick == null) return null;
        IngredientData wrongIngredient = outsiderIngredient(c, pick.recipe, pick.ingredient, r);
        if (wrongIngredient == null) return null;
        String correct = wrongIngredient.name + " - " + pick.ingredient.quantity;
        return question(id, level, "Qual alternativa traz quantidade correta, mas ingrediente errado para \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, ingredientQuantityOptions(c, pick.recipe), 3));
    }

    private static Question correctIngredientWrongUnit(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, IngredientData::hasMeasuredUnit, r);
        if (pick == null) return null;
        String wrongUnit = any(unitDistractors(pick.ingredient.unit(), 3), r);
        if (wrongUnit == null) return null;
        String correct = pick.ingredient.name + " - " + wrongUnit;
        return question(id, level, "Qual alternativa traz ingrediente correto, mas unidade errada em \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, ingredientUnitOptions(c, pick.recipe), 3));
    }

    private static Question correctCategoryIntruderIngredient(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, IngredientData::hasCategory, r);
        if (pick == null) return null;
        IngredientData intruder = outsiderIngredient(c, pick.recipe, pick.ingredient.category, r);
        if (intruder == null) return null;
        String correct = intruder.name + " - " + pick.ingredient.category;
        return question(id, level, "Qual alternativa traz categoria correta, mas ingrediente intruso para \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, categoryIngredientOptions(c, pick.recipe), 3));
    }

    private static Question similarIngredientName(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> !similarNames(c, i.name).isEmpty(), r);
        if (pick == null) return null;
        return question(id, level, "Qual ingrediente de nome parecido realmente pertence a \"" + pick.recipe.name + "\"?", pick.ingredient.name, similarNames(c, pick.ingredient.name));
    }

    private static Question confusedIngredientBetweenRecipes(ContextData c, int id, int level, Random r) {
        RecipePair pair = pairWithDifference(c, r);
        if (pair == null || pair.onlyA.isEmpty()) return null;
        IngredientData correct = any(pair.onlyA, r);
        return question(id, level, "Qual ingrediente pertence a \"" + pair.a.name + "\", mas pode ser confundido com ingredientes de \"" + pair.b.name + "\"?", correct.name, namesFrom(pair.onlyB, c.allIngredientNames, correct.name, 3));
    }

    private static Question commonIngredientDifferentQuantity(ContextData c, int id, int level, Random r) {
        for (String name : shuffled(c.allIngredientNames, r)) {
            ArrayList<IngredientData> matches = ingredientsByName(c, name);
            for (IngredientData a : matches) {
                for (IngredientData b : matches) {
                    if (a.recipeId != b.recipeId && a.hasQuantity() && b.hasQuantity() && !norm(a.quantity).equals(norm(b.quantity))) {
                        return question(id, level, "Qual ingrediente aparece em duas receitas, porem com quantidades diferentes?", name, ingredientNameDistractors(c, name, 3));
                    }
                }
            }
        }
        return null;
    }

    private static Question threeStepSequence(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithSteps(c, 4, r);
        if (recipe == null) return null;
        int start = r.nextInt(recipe.steps.size() - 2);
        String correct = sequence(recipe.steps, start, start + 3);
        return question(id, level, "Qual sequencia entre tres etapas esta realmente correta em \"" + recipe.name + "\"?", correct, sequenceDistractors(recipe, correct, 3, r));
    }

    private static Question fullIngredientCategoryUnitCombo(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasQuantity() && i.hasMeasuredUnit() && i.hasCategory(), r);
        if (pick == null) return null;
        String correct = pick.ingredient.name + " - " + pick.ingredient.quantity + " - " + pick.ingredient.unit() + " - " + pick.ingredient.category;
        return question(id, level, "Qual combinacao completa ingrediente + quantidade + unidade + categoria pertence a \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, fullComboOptions(c, pick.recipe), 3));
    }

    private static Question comboWithOneWrongField(ContextData c, int id, int level, Random r) {
        IngredientPick pick = pickIngredient(c, i -> i.hasQuantity() && i.hasCategory(), r);
        if (pick == null) return null;
        String wrongCategory = any(without(c.allCategories, listOf(pick.ingredient.category)), r);
        if (wrongCategory == null) return null;
        String correct = pick.ingredient.name + " - " + pick.ingredient.quantity + " - " + wrongCategory;
        return question(id, level, "Qual combinacao tem apenas um campo incorreto em \"" + pick.recipe.name + "\"?", correct, comboDistractors(correct, fullComboOptions(c, pick.recipe), 3));
    }

    private static Question onlyRecipeIngredientsOption(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 3, r);
        if (recipe == null) return null;
        ArrayList<String> names = ingredientNames(recipe.ingredients, "", recipe.ingredients.size());
        Collections.shuffle(names, r);
        String correct = names.get(0) + " + " + names.get(1) + " + " + names.get(2);
        return question(id, level, "Qual alternativa contem somente ingredientes de \"" + recipe.name + "\"?", correct, comboDistractors(correct, recipeTripleDistractors(c, recipe), 3));
    }

    private static Question correctAndOtherRecipeIngredient(ContextData c, int id, int level, Random r) {
        RecipeData recipe = recipeWithIngredients(c, 1, r);
        if (recipe == null) return null;
        IngredientData outside = outsiderIngredient(c, recipe, null, r);
        if (outside == null) return null;
        IngredientData inside = any(recipe.ingredients, r);
        String correct = inside.name + " + " + outside.name;
        return question(id, level, "Qual alternativa contem um ingrediente correto de \"" + recipe.name + "\" misturado com um ingrediente de outra receita?", correct, pairDistractors(c, recipe, correct, 3));
    }

    private static Question twoSnippetsToTwoRecipes(ContextData c, int id, int level, Random r) {
        ArrayList<RecipeData> withPrep = new ArrayList<>();
        for (RecipeData recipe : c.recipes) if (!recipe.steps.isEmpty()) withPrep.add(recipe);
        if (withPrep.size() < 2) return null;
        Collections.shuffle(withPrep, r);
        RecipeData a = withPrep.get(0);
        RecipeData b = withPrep.get(1);
        String correct = a.name + " / " + b.name;
        String prompt = "Trecho 1: \"" + shortText(a.steps.get(0)) + "\". Trecho 2: \"" + shortText(b.steps.get(0)) + "\". Quais receitas correspondem aos trechos?";
        return question(id, level, prompt, correct, recipePairDistractors(c, correct, 3));
    }

    private static Question question(int modelId, int level, String prompt, String correct, List<String> distractors) {
        if (correct == null || correct.trim().isEmpty() || distractors == null) return null;
        ArrayList<String> options = new ArrayList<>();
        addUnique(options, correct);
        for (String value : distractors) {
            if (options.size() >= 4) break;
            addUnique(options, value);
        }
        if (options.size() < 4) return null;
        Collections.shuffle(options);
        return new Question(modelId, level, prompt, options, indexOf(options, correct));
    }

    private static Question recipeByCount(ContextData c, int id, int level, Random r, boolean more, String prompt) {
        if (c.recipes.size() < 4) return null;
        ArrayList<RecipeData> pool = shuffled(c.recipes, r);
        pool.sort((a, b) -> more ? b.ingredients.size() - a.ingredients.size() : a.ingredients.size() - b.ingredients.size());
        RecipeData correct = pool.get(0);
        if (pool.get(0).ingredients.size() == pool.get(1).ingredients.size()) return null;
        return question(id, level, prompt, correct.name, recipeNames(pool.subList(1, pool.size()), correct.name, 3));
    }

    private static Question recipeWhereIngredientQuantityExtreme(ContextData c, int id, int level, Random r, boolean more) {
        for (String ingredientName : shuffled(c.allIngredientNames, r)) {
            ArrayList<IngredientData> matches = ingredientsByName(c, ingredientName);
            ArrayList<IngredientData> measured = new ArrayList<>();
            for (IngredientData item : matches) if (!Double.isNaN(item.quantityNumber())) measured.add(item);
            if (measured.size() < 2) continue;
            measured.sort((a, b) -> Double.compare(a.quantityNumber(), b.quantityNumber()));
            IngredientData correct = more ? measured.get(measured.size() - 1) : measured.get(0);
            IngredientData compare = more ? measured.get(measured.size() - 2) : measured.get(1);
            if (correct.quantityNumber() == compare.quantityNumber()) continue;
            RecipeData recipe = c.recipeById(correct.recipeId);
            String prompt = more ? "Em qual receita \"" + ingredientName + "\" aparece com maior quantidade?" : "Em qual receita \"" + ingredientName + "\" aparece com menor quantidade?";
            return question(id, level, prompt, recipe.name, recipeDistractors(c, recipe.name, 3));
        }
        return null;
    }

    private static Question stepByWords(ContextData c, int id, int level, Random r, String[] words, String prompt) {
        for (RecipeData recipe : shuffled(c.recipes, r)) {
            for (String step : recipe.steps) {
                for (String word : words) {
                    if (norm(step).contains(norm(word))) {
                        return question(id, level, prompt, step, stepDistractors(c, recipe, step, 3));
                    }
                }
            }
        }
        return null;
    }

    private static RecipeData recipeWithIngredients(ContextData c, int min, Random r) {
        return recipeWith(c, recipe -> recipe.ingredients.size() >= min, r);
    }

    private static RecipeData recipeWithSteps(ContextData c, int min, Random r) {
        return recipeWith(c, recipe -> recipe.steps.size() >= min, r);
    }

    private static RecipeData recipeWith(ContextData c, RecipeRule rule, Random r) {
        ArrayList<RecipeData> pool = new ArrayList<>();
        for (RecipeData recipe : c.recipes) if (rule.accept(recipe)) pool.add(recipe);
        return any(pool, r);
    }

    private static IngredientPick pickIngredient(ContextData c, IngredientRule rule, Random r) {
        ArrayList<IngredientPick> pool = new ArrayList<>();
        for (RecipeData recipe : c.recipes) {
            for (IngredientData ingredient : recipe.ingredients) {
                if (rule.accept(ingredient)) pool.add(new IngredientPick(recipe, ingredient));
            }
        }
        return any(pool, r);
    }

    private static IngredientData outsiderIngredient(ContextData c, RecipeData recipe, Object preference, Random r) {
        ArrayList<IngredientData> pool = new ArrayList<>();
        for (IngredientData ingredient : c.allIngredients) {
            if (recipeHasIngredient(recipe, ingredient.name)) continue;
            if (preference instanceof String && !((String) preference).isEmpty() && !norm(ingredient.category).equals(norm((String) preference))) continue;
            pool.add(ingredient);
        }
        if (pool.isEmpty() && preference instanceof String) return outsiderIngredient(c, recipe, null, r);
        return any(pool, r);
    }

    private static ArrayList<String> ingredientDistractors(ContextData c, RecipeData recipe, IngredientData correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        if (correct.hasCategory()) {
            for (IngredientData item : c.allIngredients) {
                if (!recipeHasIngredient(recipe, item.name) && norm(item.category).equals(norm(correct.category))) addUnique(out, item.name);
            }
        }
        for (IngredientData item : c.allIngredients) if (!recipeHasIngredient(recipe, item.name)) addUnique(out, item.name);
        for (IngredientData item : recipe.ingredients) if (item.id != correct.id) addUnique(out, item.name);
        return firstWithout(out, correct.name, count);
    }

    private static ArrayList<String> ingredientNameDistractors(ContextData c, String correct, int count) {
        return firstWithout(c.allIngredientNames, correct, count);
    }

    private static ArrayList<String> quantityDistractors(ContextData c, String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        QuantityParts parts = QuantityParts.parse(correct);
        if (!Double.isNaN(parts.number) && !parts.unit.isEmpty()) {
            for (double delta : new double[]{-2, -1, 1, 2, 5, 10}) {
                double value = parts.number + delta;
                if (value > 0) addUnique(out, formatNumber(value) + " " + parts.unit);
            }
        }
        for (String value : c.allQuantities) addUnique(out, value);
        for (String value : new String[]{"1 un", "2 un", "100 ml", "200 ml", "500 ml", "1 kg", "100 g", "1 L", "a gosto"}) addUnique(out, value);
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> unitDistractors(String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (String unit : new String[]{"un", "kg", "g", "ml", "L", "a gosto"}) addUnique(out, unit);
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> categoryDistractors(ContextData c, String correct, int count) {
        ArrayList<String> out = new ArrayList<>(c.allCategories);
        for (String value : new String[]{"base", "gordura", "tempero", "molho", "proteina", "finalizacao"}) addUnique(out, value);
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> recipeDistractors(ContextData c, String correct, int count) {
        return firstWithout(c.recipeNames, correct, count);
    }

    private static ArrayList<String> pairDistractors(ContextData c, RecipeData recipe, String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (RecipeData other : c.recipes) {
            if (other.id == recipe.id || other.ingredients.size() < 2) continue;
            addUnique(out, pairNames(twoIngredients(other, new Random())));
        }
        for (String pair : pairs(recipe.ingredients)) addUnique(out, pair);
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> comboDistractors(String correct, List<String> values, int count) {
        return firstWithout(values, correct, count);
    }

    private static ArrayList<String> stepDistractors(ContextData c, RecipeData recipe, String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (String step : recipe.steps) addUnique(out, step);
        for (RecipeData other : c.recipes) {
            if (other.id == recipe.id) continue;
            for (String step : other.steps) addUnique(out, step);
        }
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> prepSnippetDistractors(ContextData c, RecipeData recipe, String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (RecipeData other : c.recipes) {
            if (other.id != recipe.id && !other.prep.isEmpty()) addUnique(out, prepSnippet(other.prep));
        }
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> sequenceDistractors(RecipeData recipe, String correct, int count, Random r) {
        ArrayList<String> out = new ArrayList<>();
        ArrayList<String> steps = new ArrayList<>(recipe.steps);
        for (int i = 0; i < 8; i++) {
            Collections.shuffle(steps, r);
            addUnique(out, sequence(steps, 0, Math.min(3, steps.size())));
        }
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> stepOptions(List<String> steps, String correct, int count) {
        ArrayList<String> out = new ArrayList<>(steps);
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> recipePairDistractors(ContextData c, String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < c.recipes.size(); i++) {
            for (int j = i + 1; j < c.recipes.size(); j++) addUnique(out, c.recipes.get(i).name + " + " + c.recipes.get(j).name);
        }
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> quantityIngredientOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : recipe.ingredients) if (item.hasQuantity()) addUnique(out, quantityIngredient(item));
        for (IngredientData item : c.allIngredients) if (item.hasQuantity()) addUnique(out, quantityIngredient(item));
        return out;
    }

    private static ArrayList<String> categoryIngredientOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : recipe.ingredients) if (item.hasCategory()) addUnique(out, categoryIngredient(item));
        for (IngredientData item : c.allIngredients) if (item.hasCategory()) addUnique(out, categoryIngredient(item));
        return out;
    }

    private static ArrayList<String> ingredientUnitOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : recipe.ingredients) if (item.hasMeasuredUnit()) addUnique(out, ingredientUnit(item));
        for (IngredientData item : c.allIngredients) if (item.hasMeasuredUnit()) addUnique(out, ingredientUnit(item));
        return out;
    }

    private static ArrayList<String> ingredientQuantityOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : recipe.ingredients) if (item.hasQuantity()) addUnique(out, item.name + " - " + item.quantity);
        for (IngredientData item : c.allIngredients) if (item.hasQuantity()) addUnique(out, item.name + " - " + item.quantity);
        return out;
    }

    private static ArrayList<String> ingredientQuantityUnitOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : recipe.ingredients) if (item.hasQuantity() && item.hasMeasuredUnit()) addUnique(out, ingredientQuantityUnit(item));
        for (IngredientData item : c.allIngredients) if (item.hasQuantity() && item.hasMeasuredUnit()) addUnique(out, ingredientQuantityUnit(item));
        return out;
    }

    private static ArrayList<String> linkedRecipeIngredientOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData linked : items(recipe, IngredientData::isLinked)) {
            for (IngredientData item : recipe.ingredients) if (item.id != linked.id) addUnique(out, linked.linkedRecipeName + " + " + item.name);
        }
        for (RecipeData other : c.recipes) {
            if (other.id != recipe.id) {
                for (IngredientData linked : items(other, IngredientData::isLinked)) {
                    for (IngredientData item : other.ingredients) if (item.id != linked.id) addUnique(out, linked.linkedRecipeName + " + " + item.name);
                }
            }
        }
        return out;
    }

    private static ArrayList<String> linkedFullOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : c.allIngredients) {
            if (item.isLinked() && item.hasQuantity() && item.hasMeasuredUnit()) addUnique(out, ingredientQuantityUnit(item) + " -> " + item.linkedRecipeName);
        }
        return out;
    }

    private static ArrayList<String> ingredientCategoryUnitOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : c.allIngredients) if (item.hasCategory() && item.hasMeasuredUnit()) addUnique(out, item.name + " - " + item.category + " - " + item.unit());
        return out;
    }

    private static ArrayList<String> fullComboOptions(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : c.allIngredients) {
            if (item.hasQuantity() && item.hasMeasuredUnit() && item.hasCategory()) addUnique(out, item.name + " - " + item.quantity + " - " + item.unit() + " - " + item.category);
        }
        return out;
    }

    private static ArrayList<String> recipeTripleDistractors(ContextData c, RecipeData recipe) {
        ArrayList<String> out = new ArrayList<>();
        for (RecipeData other : c.recipes) {
            if (other.ingredients.size() >= 3) {
                ArrayList<String> names = ingredientNames(other.ingredients, "", other.ingredients.size());
                addUnique(out, names.get(0) + " + " + names.get(1) + " + " + names.get(2));
            }
        }
        IngredientData outsider = outsiderIngredient(c, recipe, null, new Random());
        if (outsider != null && recipe.ingredients.size() >= 2) {
            addUnique(out, recipe.ingredients.get(0).name + " + " + recipe.ingredients.get(1).name + " + " + outsider.name);
        }
        return out;
    }

    private static ArrayList<String> namesFrom(List<IngredientData> preferred, List<String> fallback, String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : preferred) addUnique(out, item.name);
        for (String value : fallback) addUnique(out, value);
        return firstWithout(out, correct, count);
    }

    private static ArrayList<String> recipesWithDifferentCount(ContextData c, int count, String correct, int wanted) {
        ArrayList<String> out = new ArrayList<>();
        for (RecipeData recipe : c.recipes) if (recipe.ingredients.size() != count) addUnique(out, recipe.name);
        return firstWithout(out, correct, wanted);
    }

    private static ArrayList<String> recipesByCategoryCount(ContextData c, String category, int exact, String correct, int wanted) {
        ArrayList<String> out = new ArrayList<>();
        for (RecipeData recipe : c.recipes) if (countCategory(recipe, category) != exact) addUnique(out, recipe.name);
        return firstWithout(out, correct, wanted);
    }

    private static ArrayList<String> similarNames(ContextData c, String name) {
        ArrayList<String> out = new ArrayList<>();
        String n = norm(name);
        for (String other : c.allIngredientNames) {
            String o = norm(other);
            if (!o.equals(n) && (o.contains(n) || n.contains(o) || firstWord(o).equals(firstWord(n)))) addUnique(out, other);
        }
        return firstWithout(out, name, 3);
    }

    private static ArrayList<String> numberDistractors(int correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        for (int value : new int[]{correct - 2, correct - 1, correct + 1, correct + 2, correct + 3}) {
            if (value > 0 && value != correct) addUnique(out, countLabel(value));
        }
        return firstWithout(out, countLabel(correct), count);
    }

    private static ArrayList<String> firstWithout(List<String> values, String correct, int count) {
        ArrayList<String> out = new ArrayList<>();
        ArrayList<String> pool = new ArrayList<>();
        for (String value : values) {
            if (!norm(value).equals(norm(correct))) addUnique(pool, value);
        }
        Collections.shuffle(pool);
        for (String value : pool) {
            if (out.size() >= count) break;
            addUnique(out, value);
        }
        return out;
    }

    private static ArrayList<String> without(List<String> source, List<String> excluded) {
        ArrayList<String> out = new ArrayList<>();
        for (String value : source) {
            if (!containsNorm(excluded, value)) addUnique(out, value);
        }
        return out;
    }

    private static ArrayList<String> listOf(String value) {
        ArrayList<String> out = new ArrayList<>();
        addUnique(out, value);
        return out;
    }

    private static String quantityIngredient(IngredientData item) { return item.quantity + " - " + item.name; }
    private static String categoryIngredient(IngredientData item) { return item.category + " - " + item.name; }
    private static String ingredientUnit(IngredientData item) { return item.name + " - " + item.unit(); }
    private static String ingredientQuantityUnit(IngredientData item) { return item.name + " - " + item.quantity + " - " + item.unit(); }

    private static String nearbyQuantity(IngredientData item) {
        QuantityParts parts = QuantityParts.parse(item.quantity);
        if (Double.isNaN(parts.number) || parts.unit.isEmpty()) return "";
        double value = parts.number + (parts.number >= 5 ? 2 : 1);
        return formatNumber(value) + " " + parts.unit;
    }

    private static String countLabel(int count) {
        return count + (count == 1 ? " ingrediente" : " ingredientes");
    }

    private static String pairNames(List<IngredientData> items) {
        return items.get(0).name + " + " + items.get(1).name;
    }

    private static List<IngredientData> twoIngredients(RecipeData recipe, Random r) {
        ArrayList<IngredientData> list = new ArrayList<>(recipe.ingredients);
        Collections.shuffle(list, r);
        return list.subList(0, 2);
    }

    private static ArrayList<String> pairs(List<IngredientData> items) {
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) addUnique(out, items.get(i).name + " + " + items.get(j).name);
        }
        return out;
    }

    private static String sequence(List<String> steps, int start, int end) {
        ArrayList<String> out = new ArrayList<>();
        for (int i = start; i < end && i < steps.size(); i++) out.add(shortText(steps.get(i)));
        return join(out, " -> ");
    }

    private static String stepFromOtherRecipe(ContextData c, RecipeData recipe, Random r) {
        ArrayList<String> steps = new ArrayList<>();
        for (RecipeData other : c.recipes) if (other.id != recipe.id) steps.addAll(other.steps);
        return any(steps, r);
    }

    private static PrepIngredient prepIngredient(ContextData c, Random r) {
        ArrayList<PrepIngredient> pool = new ArrayList<>();
        for (RecipeData recipe : c.recipes) {
            for (String step : recipe.steps) {
                for (IngredientData ingredient : recipe.ingredients) {
                    if (norm(step).contains(norm(ingredient.name))) pool.add(new PrepIngredient(recipe, ingredient, step));
                }
            }
        }
        return any(pool, r);
    }

    private static ArrayList<IngredientData> ingredientsInStep(RecipeData recipe, String step) {
        ArrayList<IngredientData> out = new ArrayList<>();
        for (IngredientData ingredient : recipe.ingredients) {
            if (norm(step).contains(norm(ingredient.name))) out.add(ingredient);
        }
        return out;
    }

    private static RecipePair pairWithDifference(ContextData c, Random r) {
        ArrayList<RecipePair> pairs = new ArrayList<>();
        for (int i = 0; i < c.recipes.size(); i++) {
            for (int j = i + 1; j < c.recipes.size(); j++) {
                RecipePair pair = new RecipePair(c.recipes.get(i), c.recipes.get(j));
                if (!pair.onlyA.isEmpty() && !pair.onlyB.isEmpty()) pairs.add(pair);
            }
        }
        return any(pairs, r);
    }

    private static RecipePair pairWithCommon(ContextData c, Random r) {
        ArrayList<RecipePair> pairs = new ArrayList<>();
        for (int i = 0; i < c.recipes.size(); i++) {
            for (int j = i + 1; j < c.recipes.size(); j++) {
                RecipePair pair = new RecipePair(c.recipes.get(i), c.recipes.get(j));
                if (!pair.common.isEmpty()) pairs.add(pair);
            }
        }
        return any(pairs, r);
    }

    private static RecipePair pairWithoutCommon(ContextData c, Random r) {
        ArrayList<RecipePair> pairs = new ArrayList<>();
        for (int i = 0; i < c.recipes.size(); i++) {
            for (int j = i + 1; j < c.recipes.size(); j++) {
                RecipePair pair = new RecipePair(c.recipes.get(i), c.recipes.get(j));
                if (pair.common.isEmpty()) pairs.add(pair);
            }
        }
        return any(pairs, r);
    }

    private static boolean recipeContainsLinked(RecipeData recipe, int linkedRecipeId) {
        for (IngredientData item : recipe.ingredients) if (item.linkedRecipeId == linkedRecipeId) return true;
        return false;
    }

    private static boolean recipeHasIngredient(RecipeData recipe, String ingredientName) {
        for (IngredientData item : recipe.ingredients) if (norm(item.name).equals(norm(ingredientName))) return true;
        return false;
    }

    private static boolean recipeHasUnit(RecipeData recipe, String unit) {
        for (IngredientData item : recipe.ingredients) if (norm(item.unit()).equals(norm(unit))) return true;
        return false;
    }

    private static boolean hasCategoryWord(IngredientData item, String word) {
        return norm(item.category).contains(norm(word));
    }

    private static int countCategory(RecipeData recipe, String category) {
        int count = 0;
        for (IngredientData item : recipe.ingredients) if (norm(item.category).equals(norm(category))) count++;
        return count;
    }

    private static ArrayList<IngredientData> ingredientsByName(ContextData c, String name) {
        ArrayList<IngredientData> out = new ArrayList<>();
        for (IngredientData item : c.allIngredients) if (norm(item.name).equals(norm(name))) out.add(item);
        return out;
    }

    private static ArrayList<IngredientData> items(RecipeData recipe, IngredientRule rule) {
        ArrayList<IngredientData> out = new ArrayList<>();
        for (IngredientData item : recipe.ingredients) if (rule.accept(item)) out.add(item);
        return out;
    }

    private static ArrayList<String> ingredientNames(List<IngredientData> ingredients, String correct, int limit) {
        ArrayList<String> out = new ArrayList<>();
        for (IngredientData item : ingredients) {
            if (out.size() >= limit) break;
            if (!norm(item.name).equals(norm(correct))) addUnique(out, item.name);
        }
        return out;
    }

    private static ArrayList<String> recipeNames(List<RecipeData> recipes, String correct, int limit) {
        ArrayList<String> out = new ArrayList<>();
        for (RecipeData recipe : recipes) {
            if (out.size() >= limit) break;
            if (!norm(recipe.name).equals(norm(correct))) addUnique(out, recipe.name);
        }
        return out;
    }

    private static <T> ArrayList<T> shuffled(List<T> values, Random r) {
        ArrayList<T> out = new ArrayList<>(values);
        Collections.shuffle(out, r);
        return out;
    }

    private static <T> T any(List<T> values, Random r) {
        if (values == null || values.isEmpty()) return null;
        return values.get(r.nextInt(values.size()));
    }

    private static void addUnique(List<String> values, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (!containsNorm(values, value)) values.add(value.trim());
    }

    private static boolean containsNorm(List<String> values, String value) {
        String wanted = norm(value);
        for (String existing : values) if (norm(existing).equals(wanted)) return true;
        return false;
    }

    private static int indexOf(List<String> values, String value) {
        String wanted = norm(value);
        for (int i = 0; i < values.size(); i++) if (norm(values.get(i)).equals(wanted)) return i;
        return 0;
    }

    private static String norm(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private static String prepSnippet(String value) {
        return shortText(value == null ? "" : value.replace("\r", " ").replace("\n", " "));
    }

    private static String shortText(String value) {
        String clean = value == null ? "" : value.trim();
        while (clean.contains("  ")) clean = clean.replace("  ", " ");
        if (clean.length() > 92) return clean.substring(0, 89).trim() + "...";
        return clean;
    }

    private static String replaceIgnoreCase(String text, String target, String replacement) {
        String nText = norm(text);
        String nTarget = norm(target);
        int index = nText.indexOf(nTarget);
        if (index < 0) return text;
        return text.substring(0, index) + replacement + text.substring(Math.min(text.length(), index + target.length()));
    }

    private static String join(List<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(separator);
            out.append(value);
        }
        return out.toString();
    }

    private static String firstWord(String value) {
        String clean = value == null ? "" : value.trim();
        int index = clean.indexOf(' ');
        return index < 0 ? clean : clean.substring(0, index);
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001) return String.valueOf((long) Math.round(value));
        return String.format(Locale.US, "%.1f", value).replace(".", ",");
    }

    interface RecipeRule { boolean accept(RecipeData recipe); }
    interface IngredientRule { boolean accept(IngredientData ingredient); }

    static final class Question {
        final int modelId;
        final int level;
        final String prompt;
        final ArrayList<String> options;
        final int correctIndex;

        Question(int modelId, int level, String prompt, ArrayList<String> options, int correctIndex) {
            this.modelId = modelId;
            this.level = level;
            this.prompt = prompt;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    static final class RecipeData {
        final int id;
        final String name;
        final String prep;
        final ArrayList<IngredientData> ingredients = new ArrayList<>();
        final ArrayList<String> steps;

        RecipeData(int id, String name, String prep) {
            this.id = id;
            this.name = safe(name);
            this.prep = safe(prep);
            this.steps = splitSteps(prep);
        }
    }

    static final class IngredientData {
        final int id;
        final int recipeId;
        final int linkedRecipeId;
        final String name;
        final String quantity;
        final String category;
        final String linkedRecipeName;

        IngredientData(int id, int recipeId, String name, String quantity, String category, int linkedRecipeId, String linkedRecipeName) {
            this.id = id;
            this.recipeId = recipeId;
            this.name = safe(name);
            this.quantity = safe(quantity);
            this.category = safe(category);
            this.linkedRecipeId = linkedRecipeId;
            this.linkedRecipeName = safe(linkedRecipeName);
        }

        boolean hasQuantity() { return !quantity.isEmpty(); }
        boolean hasCategory() { return !category.isEmpty(); }
        boolean isLinked() { return linkedRecipeId > 0; }
        boolean isAGosto() { return norm(quantity).contains("a gosto"); }
        boolean hasUnit() { return !unit().isEmpty(); }
        boolean hasMeasuredUnit() { return hasUnit() && !isAGosto(); }
        double quantityNumber() { return QuantityParts.parse(quantity).number; }
        String unit() { return QuantityParts.parse(quantity).unit; }
    }

    private static final class ContextData {
        final ArrayList<RecipeData> recipes = new ArrayList<>();
        final ArrayList<IngredientData> allIngredients = new ArrayList<>();
        final ArrayList<String> recipeNames = new ArrayList<>();
        final ArrayList<String> allIngredientNames = new ArrayList<>();
        final ArrayList<String> allQuantities = new ArrayList<>();
        final ArrayList<String> allCategories = new ArrayList<>();

        ContextData(List<RecipeData> source) {
            if (source != null) recipes.addAll(source);
            for (RecipeData recipe : recipes) {
                addUnique(recipeNames, recipe.name);
                for (IngredientData ingredient : recipe.ingredients) {
                    allIngredients.add(ingredient);
                    addUnique(allIngredientNames, ingredient.name);
                    if (ingredient.hasQuantity()) addUnique(allQuantities, ingredient.quantity);
                    if (ingredient.hasCategory()) addUnique(allCategories, ingredient.category);
                }
            }
        }

        RecipeData recipeById(int id) {
            for (RecipeData recipe : recipes) if (recipe.id == id) return recipe;
            return new RecipeData(0, "", "");
        }
    }

    private static final class IngredientPick {
        final RecipeData recipe;
        final IngredientData ingredient;
        IngredientPick(RecipeData recipe, IngredientData ingredient) {
            this.recipe = recipe;
            this.ingredient = ingredient;
        }
    }

    private static final class PrepIngredient {
        final RecipeData recipe;
        final IngredientData ingredient;
        final String step;
        PrepIngredient(RecipeData recipe, IngredientData ingredient, String step) {
            this.recipe = recipe;
            this.ingredient = ingredient;
            this.step = step;
        }
    }

    private static final class RecipePair {
        final RecipeData a;
        final RecipeData b;
        final ArrayList<IngredientData> onlyA = new ArrayList<>();
        final ArrayList<IngredientData> onlyB = new ArrayList<>();
        final ArrayList<IngredientData> common = new ArrayList<>();

        RecipePair(RecipeData a, RecipeData b) {
            this.a = a;
            this.b = b;
            for (IngredientData item : a.ingredients) {
                if (recipeHasIngredient(b, item.name)) common.add(item);
                else onlyA.add(item);
            }
            for (IngredientData item : b.ingredients) {
                if (!recipeHasIngredient(a, item.name)) onlyB.add(item);
            }
        }
    }

    private static final class QuantityParts {
        final double number;
        final String unit;

        QuantityParts(double number, String unit) {
            this.number = number;
            this.unit = unit;
        }

        static QuantityParts parse(String value) {
            String clean = safe(value).trim();
            String n = norm(clean);
            if (n.isEmpty()) return new QuantityParts(Double.NaN, "");
            if (n.contains("a gosto")) return new QuantityParts(Double.NaN, "a gosto");
            String unit = "";
            for (String candidate : new String[]{"kg", "ml", "un", "g"}) {
                if (n.endsWith(candidate)) {
                    unit = candidate;
                    break;
                }
            }
            if (n.endsWith("l") && !n.endsWith("ml")) unit = "L";
            String numberText = clean;
            if (!unit.isEmpty()) numberText = clean.substring(0, Math.max(0, clean.length() - unit.length())).trim();
            numberText = numberText.replace(",", ".");
            try {
                return new QuantityParts(Double.parseDouble(numberText), unit);
            } catch (Exception e) {
                return new QuantityParts(Double.NaN, unit);
            }
        }
    }

    private static ArrayList<String> splitSteps(String prep) {
        ArrayList<String> out = new ArrayList<>();
        String clean = safe(prep).replace("\r", "\n");
        String[] parts = clean.split("[\\n.;]+");
        for (String part : parts) {
            String step = part.trim();
            if (step.length() >= 8) addUnique(out, shortText(step));
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
