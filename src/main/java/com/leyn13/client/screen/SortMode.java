package com.leyn13.client.screen;

/**
 * Modes de tri disponibles dans l'écran des progrès.
 */
public enum SortMode {
	DEFAULT("Ordre vanilla"),
	A_TO_Z("A → Z"),
	PROGRESS("Progression"),
	INCOMPLETE("Inachevés");

	private final String label;

	SortMode(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public SortMode next() {
		SortMode[] v = values();
		return v[(ordinal() + 1) % v.length];
	}
}
