/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2021 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.HashSet;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class NamedExpressionListInput extends ListInput<ArrayList<NamedExpression>> {

	public NamedExpressionListInput(String key, String cat, ArrayList<NamedExpression> def) {
		super(key, cat, def);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addEnclosure("{", str, "}");
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<NamedExpression> temp = new ArrayList<>(subArgs.size());

		// Ensure that no custom output names are repeated
		HashSet<String> nameSet = new HashSet<>();
		for (KeywordIndex subArg : subArgs) {
			if (subArg.numArgs() == 0)
				continue;
			String name = subArg.getArg(0);
			if (nameSet.contains(name))
				throw new InputErrorException("Duplicate custom output name: %s", name);
			nameSet.add(name);
		}

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2, 3);
			try {
				// Parse the expression name
				String name = subArg.getArg(0);
				if (OutputHandle.hasOutput(thisEnt.getClass(), name)
						|| thisEnt.hasAttribute(name)) {
					throw new InputErrorException("Custom output name is the same as existing output name: %s", name);
				}

				String expString = subArg.getArg(1);
				Expression exp = ExpParser.parseExpression(ExpEvaluator.getParseContext(thisEnt, expString), expString);

				Class<? extends Unit> unitType = DimensionlessUnit.class;
				if (subArg.numArgs() == 3) {
					unitType = Input.parseUnitType(thisEnt.getJaamSimModel(), subArg.getArg(2));
				}
				if (unitType != DimensionlessUnit.class) {
					ExpParser.assertResultType(exp, ExpResType.NUMBER);
				}
				ExpParser.assertUnitType(exp, unitType);

				// Save the data for this expression
				NamedExpression ne = new NamedExpression(name, exp, unitType);
				temp.add(ne);

			} catch (ExpError e) {
				throw new InputErrorException(e);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}

		// Save the data for each attribute
		value = temp;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_CUSTOM_OUT;
	}

	@Override
	public void copyFrom(Entity thisEnt, Input<?> in) {
		super.copyFrom(thisEnt, in);

		// An expression input must be re-parsed to reset the entity referred to by "this"
		parseFrom(thisEnt, in);
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty())
			return "";

		return defValue.toString();
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	public String getStubDefinition() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (NamedExpression ne : value) {
			if (first) {
				first = false;
			}
			else {
				sb.append(Input.BRACE_SEPARATOR);
			}
			sb.append(ne.getStubDefinition());
		}
		return sb.toString();
	}

}
