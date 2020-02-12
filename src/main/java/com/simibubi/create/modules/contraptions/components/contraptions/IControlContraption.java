package com.simibubi.create.modules.contraptions.components.contraptions;

public interface IControlContraption {

	public void attach(ContraptionEntity contraption);
	
	default void onStall() {
	}

	public boolean isValid();
	
}
