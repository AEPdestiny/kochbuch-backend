package de.htwberlin.webtech.recipe.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpoonacularAnalyzedInstruction {

    private List<SpoonacularInstructionStep> steps;

    public List<SpoonacularInstructionStep> getSteps() {
        return steps;
    }

    public void setSteps(List<SpoonacularInstructionStep> steps) {
        this.steps = steps;
    }
}
