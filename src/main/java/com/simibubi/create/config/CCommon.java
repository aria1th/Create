package com.simibubi.create.config;

public class CCommon extends ConfigBase {

	public CWorldGen worldGen = nested(0, CWorldGen::new, Comments.worldGen);

	@Override
	public String getName() {
		return "common";
	}

	private static class Comments {
		static String worldGen = "Modify Create's impact on your terrain";
	}

}
