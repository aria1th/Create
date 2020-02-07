package com.simibubi.create.config;

public class CServer extends ConfigBase {

	public ConfigGroup modules = group(0, "modules", Comments.modules);
	public ConfigBool enableSchematics = b(true, "enableSchematics");
	public ConfigBool enableCuriosities = b(true, "enableCuriosities");
	public ConfigBool enablePalettes = b(true, "enablePalettes");
	public ConfigBool enableLogistics = b(true, "enableLogistics");

	public CKinetics kinetics = nested(0, CKinetics::new, Comments.kinetics);
	public CLogistics logistics = nested(0, CLogistics::new, Comments.logistics);
	public CSchematics schematics = nested(0, CSchematics::new, Comments.schematics);
	public CCuriosities curiosities = nested(0, CCuriosities::new, Comments.curiosities);
	public CDamageControl control = nested(0, CDamageControl::new, Comments.control);

	@Override
	public String getName() {
		return "server";
	}

	private static class Comments {
		static String schematics = "The Schematics Module";
		static String kinetics = "The Contraptions Module";
		static String logistics = "The Logistics Module";
		static String curiosities = "Everything that spins";
		static String modules = "Configure which Modules should be accessible in recipes and creative menus.";
		static String control = "You can try inhibiting related game mechanics for troubleshooting repeated crashes.";
	}

}
