package de.metas.contracts.refund;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.adempiere.util.Check;

import com.google.common.collect.ImmutableList;

import de.metas.bpartner.BPartnerId;
import de.metas.contracts.FlatrateTermId;
import de.metas.contracts.refund.RefundConfig.RefundMode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/*
 * #%L
 * de.metas.contracts
 * %%
 * Copyright (C) 2018 metas GmbH
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

@Value
public class RefundContract
{
	/** may be {@code null} if the contract is not persisted. */
	FlatrateTermId id;

	/** contains the refund configs, ordered by minQty descending. */
	List<RefundConfig> refundConfigs;

	LocalDate startDate;

	LocalDate endDate;

	BPartnerId bPartnerId;

	@Builder(toBuilder = true)
	private RefundContract(
			@Nullable final FlatrateTermId id,
			@NonNull final BPartnerId bPartnerId,
			@Singular final List<RefundConfig> refundConfigs,
			@NonNull final LocalDate startDate,
			@NonNull final LocalDate endDate)
	{
		this.id = id;
		this.bPartnerId = bPartnerId;
		this.startDate = startDate;
		this.endDate = endDate;

		RefundConfigs.assertValid(refundConfigs);

		this.refundConfigs = RefundConfigs.sortByMinQtyDesc(refundConfigs);
	}

	public RefundConfig getRefundConfig(@NonNull final BigDecimal qty)
	{
		return refundConfigs
				.stream()
				.filter(config -> config.getMinQty().compareTo(qty) <= 0)
				.findFirst()
				.orElse(null);
	}

	public List<RefundConfig> getRefundConfigsToApplyForQuantity(@NonNull final BigDecimal qty)
	{
		final Predicate<RefundConfig> minQtyLessOrEqual = config -> config.getMinQty().compareTo(qty) <= 0;

		return refundConfigs
				.stream()
				.filter(minQtyLessOrEqual)
				.collect(ImmutableList.toImmutableList());
	}

	public RefundConfig getRefundConfigById(@NonNull final RefundConfigId refundConfigId)
	{
		for (RefundConfig refundConfig : refundConfigs)
		{
			if (refundConfig.getId().equals(refundConfigId))
			{
				return refundConfig;
			}
		}

		Check.fail("This contract has no config with id={}; this={}", refundConfigId, this);
		return null;
	}

	public RefundMode extractRefundMode()
	{
		return RefundConfigs.extractRefundMode(refundConfigs);
	}

	public Optional<RefundConfig> getRefundConfigToUseProfitCalculation()
	{
		return getRefundConfigs()
				.stream()
				.filter(RefundConfig::isUseInProfitCalculation)
				.findFirst();
	}

}
