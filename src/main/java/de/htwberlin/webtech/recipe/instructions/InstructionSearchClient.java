package de.htwberlin.webtech.recipe.instructions;

import de.htwberlin.webtech.recipe.dto.InstructionSearchResult;

import java.util.List;

public interface InstructionSearchClient {

    List<InstructionSearchResult> search(String query);
}
