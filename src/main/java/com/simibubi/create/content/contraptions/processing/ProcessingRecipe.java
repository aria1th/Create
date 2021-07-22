package com.simibubi.create.content.contraptions.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.recipe.IRecipeTypeInfo;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ProcessingRecipe<T extends IInventory> implements IRecipe<T> {

	protected ResourceLocation id;
	protected NonNullList<Ingredient> ingredients;
	protected NonNullList<ProcessingOutput> results;
	protected NonNullList<FluidIngredient> fluidIngredients;
	protected NonNullList<FluidStack> fluidResults;
	protected int processingDuration;
	protected HeatCondition requiredHeat;

	private IRecipeType<?> type;
	private IRecipeSerializer<?> serializer;
	private IRecipeTypeInfo typeInfo;
	private Supplier<ItemStack> forcedResult;

	public ProcessingRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
		this.forcedResult = null;
		this.typeInfo = typeInfo;
		this.processingDuration = params.processingDuration;
		this.fluidIngredients = params.fluidIngredients;
		this.fluidResults = params.fluidResults;
		this.serializer = typeInfo.getSerializer();
		this.requiredHeat = params.requiredHeat;
		this.ingredients = params.ingredients;
		this.type = typeInfo.getType();
		this.results = params.results;
		this.id = params.id;

		validate(typeInfo.getId());
	}

	// Recipe type options:

	protected abstract int getMaxInputCount();

	protected abstract int getMaxOutputCount();

	protected boolean canRequireHeat() {
		return false;
	}

	protected boolean canSpecifyDuration() {
		return true;
	}

	protected int getMaxFluidInputCount() {
		return 0;
	}

	protected int getMaxFluidOutputCount() {
		return 0;
	}

	//

	private void validate(ResourceLocation recipeTypeId) {
		String messageHeader = "Your custom " + recipeTypeId + " recipe (" + id.toString() + ")";
		Logger logger = Create.LOGGER;
		int ingredientCount = ingredients.size();
		int outputCount = results.size();

		if (ingredientCount > getMaxInputCount())
			logger.warn(messageHeader + " has more item inputs (" + ingredientCount + ") than supported ("
				+ getMaxInputCount() + ").");

		if (outputCount > getMaxOutputCount())
			logger.warn(messageHeader + " has more item outputs (" + outputCount + ") than supported ("
				+ getMaxOutputCount() + ").");

		if (processingDuration > 0 && !canSpecifyDuration())
			logger.warn(messageHeader + " specified a duration. Durations have no impact on this type of recipe.");

		if (requiredHeat != HeatCondition.NONE && !canRequireHeat())
			logger.warn(
				messageHeader + " specified a heat condition. Heat conditions have no impact on this type of recipe.");

		ingredientCount = fluidIngredients.size();
		outputCount = fluidResults.size();

		if (ingredientCount > getMaxFluidInputCount())
			logger.warn(messageHeader + " has more fluid inputs (" + ingredientCount + ") than supported ("
				+ getMaxFluidInputCount() + ").");

		if (outputCount > getMaxFluidOutputCount())
			logger.warn(messageHeader + " has more fluid outputs (" + outputCount + ") than supported ("
				+ getMaxFluidOutputCount() + ").");
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	public NonNullList<FluidIngredient> getFluidIngredients() {
		return fluidIngredients;
	}

	public NonNullList<ProcessingOutput> getRollableResults() {
		return results;
	}

	public NonNullList<FluidStack> getFluidResults() {
		return fluidResults;
	}

	public List<ItemStack> getRollableResultsAsItemStacks() {
		return getRollableResults().stream()
			.map(ProcessingOutput::getStack)
			.collect(Collectors.toList());
	}

	public void enforceNextResult(Supplier<ItemStack> stack) {
		forcedResult = stack;
	}

	public List<ItemStack> rollResults() {
		List<ItemStack> results = new ArrayList<>();
		NonNullList<ProcessingOutput> rollableResults = getRollableResults();
		for (int i = 0; i < rollableResults.size(); i++) {
			ProcessingOutput output = rollableResults.get(i);
			ItemStack stack = i == 0 && forcedResult != null ? forcedResult.get() : output.rollOutput();
			if (!stack.isEmpty())
				results.add(stack);
		}
		return results;
	}

	public int getProcessingDuration() {
		return processingDuration;
	}

	public HeatCondition getRequiredHeat() {
		return requiredHeat;
	}

	// IRecipe<> paperwork

	@Override
	public ItemStack assemble(T inv) {
		return getResultItem();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem() {
		return getRollableResults().isEmpty() ? ItemStack.EMPTY
			: getRollableResults().get(0)
				.getStack();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	// Processing recipes do not show up in the recipe book
	@Override
	public String getGroup() {
		return "processing";
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return serializer;
	}

	@Override
	public IRecipeType<?> getType() {
		return type;
	}

	public IRecipeTypeInfo getTypeInfo() {
		return typeInfo;
	}

	// Additional Data added by subtypes

	public void readAdditional(JsonObject json) {}

	public void readAdditional(PacketBuffer buffer) {}

	public void writeAdditional(JsonObject json) {}

	public void writeAdditional(PacketBuffer buffer) {}

}
