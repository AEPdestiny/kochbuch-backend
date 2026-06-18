package de.htwberlin.webtech.recipe.instructions;

public class InstructionSearchNotConfiguredException extends InstructionSearchClientException {

    public InstructionSearchNotConfiguredException() {
        super("Tavily API key is not configured.");
    }
}
