package org.eevolution.api;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.math.BigDecimal;
import java.util.Date;

import org.adempiere.util.ISingletonService;
import org.compiere.model.I_M_Product;
import org.eevolution.model.I_PP_Product_BOM;
import org.eevolution.model.I_PP_Product_BOMLine;

import de.metas.product.ProductId;

public interface IProductBOMBL extends ISingletonService
{
	boolean isValidFromTo(I_PP_Product_BOM productBOM, Date date);

	boolean isValidFromTo(I_PP_Product_BOMLine bomLine, Date date);

	void setIsBOM(I_M_Product product);

	/**
	 * Calculates low level code (LLC) for given product.
	 * It also checks for BOM cycles.
	 *
	 * @param productId
	 * @return low level code (LLC)
	 */
	int calculateProductLowestLevel(int productId);

	IProductLowLevelUpdater updateProductLowLevels();

	/**
	 * Calculates Qty + Scrap
	 *
	 * @param qty qty (without scrap)
	 * @param qtyScrap scrap percent (between 0..100)
	 * @return qty * (1 + qtyScrap/100)
	 */
	BigDecimal calculateQtyWithScrap(BigDecimal qty, BigDecimal qtyScrap);

	/**
	 * Checks if a BOMLine which is a <code>X_PP_Product_BOMLine.COMPONENTTYPE_Variant</code> has a valid VariantGroup<br>
	 *
	 * Valid variant group means that exists at least one other BOMLine which has Component Type <code>X_PP_Order_BOMLine.COMPONENTTYPE_Component</code>
	 * or <code>X_PP_Order_BOMLine.COMPONENTTYPE_Packing</code> and same VariantGroup.
	 *
	 * @param bomLine
	 * @return
	 */
	boolean isValidVariantGroup(I_PP_Product_BOMLine bomLine);

	/**
	 * Return Unified BOM Qty Multiplier.
	 *
	 * i.e. how much of this component is needed for 1 item of finished good.
	 *
	 * @param productBomLine
	 * @param endProductId
	 *
	 * @return If is percentage then QtyBatch / 100 will be returned, else QtyBOM.
	 */
	BigDecimal getQtyMultiplier(I_PP_Product_BOMLine productBomLine, ProductId endProductId);

	String getBOMDescriptionForProductId(ProductId productId);
}
