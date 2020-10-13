package com.simibubi.create.compat.jei.category;

import java.util.Arrays;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedPress;
import com.simibubi.create.content.contraptions.processing.BasinRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

public class PackingCategory extends BasinCategory {

	private AnimatedPress press = new AnimatedPress(true);

	public PackingCategory() {
		super("packing", doubleItemIcon(AllBlocks.MECHANICAL_PRESS.get(), AllBlocks.BASIN.get()),
				emptyBackground(177, 110));
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, BasinRecipe recipe, IIngredients ingredients) {
		if (!recipe.convertedRecipe) {
			super.setRecipe(recipeLayout, recipe, ingredients);
			return;
		}
		
		IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
		int i = 0;

		NonNullList<Ingredient> ingredients2 = recipe.getIngredients();
		int size = ingredients2.size();
		int rows = size == 4 ? 2 : 3;
		while (i < size) {
			Ingredient ingredient = ingredients2.get(i);
			itemStacks.init(i, true, (rows == 2 ? 26 : 17) + (i % rows) * 19, 60 - (i / rows) * 19);
			itemStacks.set(i, Arrays.asList(ingredient.getMatchingStacks()));
			i++;
		}

		itemStacks.init(i, false, 141, 60);
		itemStacks.set(i, recipe.getRecipeOutput());
	}

	@Override
	public void draw(BasinRecipe recipe, double mouseX, double mouseY) {
		if (!recipe.convertedRecipe) {
			super.draw(recipe, mouseX, mouseY);
			
		} else {
			NonNullList<Ingredient> ingredients2 = recipe.getIngredients();
			int size = ingredients2.size();
			int rows = size == 4 ? 2 : 3;
			for (int i = 0; i < size; i++) 
				AllGuiTextures.JEI_SLOT.draw((rows == 2 ? 26 : 17) + (i % rows) * 19, 60 - (i / rows) * 19);
			AllGuiTextures.JEI_SLOT.draw(141, 60);
			AllGuiTextures.JEI_DOWN_ARROW.draw(136, 42);
			AllGuiTextures.JEI_SHADOW.draw(81, 67);
		}
		
		press.draw(getBackground().getWidth() / 2 + 6, 40);
	}

}
