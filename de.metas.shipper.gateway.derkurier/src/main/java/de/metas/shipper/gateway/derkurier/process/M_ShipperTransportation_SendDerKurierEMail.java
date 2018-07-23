package de.metas.shipper.gateway.derkurier.process;

import org.adempiere.ad.dao.ConstantQueryFilter;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryFilter;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.Adempiere;

import de.metas.document.engine.IDocument;
import de.metas.document.engine.IDocumentBL;
import de.metas.process.IProcessPrecondition;
import de.metas.process.IProcessPreconditionsContext;
import de.metas.process.JavaProcess;
import de.metas.process.ProcessInfo;
import de.metas.process.ProcessPreconditionsResolution;
import de.metas.shipper.gateway.derkurier.misc.DerKurierDeliveryOrderEmailer;
import de.metas.shipper.gateway.derkurier.misc.DerKurierShipperConfig;
import de.metas.shipper.gateway.derkurier.misc.DerKurierShipperConfigRepository;
import de.metas.shipping.api.ShipperTransportationId;
import de.metas.shipping.model.I_M_ShipperTransportation;
import lombok.NonNull;

/*
 * #%L
 * de.metas.shipper.gateway.derkurier
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

public class M_ShipperTransportation_SendDerKurierEMail
		extends JavaProcess
		implements IProcessPrecondition
{

	private final transient IQueryBL queryBL = Services.get(IQueryBL.class);
	private final transient IDocumentBL documentBL = Services.get(IDocumentBL.class);

	private final transient DerKurierDeliveryOrderEmailer //
	derKurierDeliveryOrderEmailer = Adempiere.getBean(DerKurierDeliveryOrderEmailer.class);

	@Override
	public ProcessPreconditionsResolution checkPreconditionsApplicable(@NonNull final IProcessPreconditionsContext context)
	{
		if (context.isNoSelection())
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("No records selected");
		}

		if (context.getSelectionSize() > 500)
		{
			// Checking is too expensive; just assume that some selected records have an email address
			return ProcessPreconditionsResolution.accept();
		}

		final boolean atLeastOneRecordHasEmail = context
				.getSelectedModels(I_M_ShipperTransportation.class)
				.stream()
				.filter(this::isCompleted)
				.anyMatch(this::hasDerKurierMailAddress);
		if (!atLeastOneRecordHasEmail)
		{
			return ProcessPreconditionsResolution.rejectWithInternalReason("No selected M_ShipperTransportation's shipper has a Der Kurier config");
		}
		return ProcessPreconditionsResolution.accept();
	}

	@Override
	protected final void prepare()
	{
		final ProcessInfo pi = getProcessInfo();

		final IQueryFilter<I_M_ShipperTransportation> filter = pi.getQueryFilterOrElse(ConstantQueryFilter.of(false));
		final int pInstanceId = getAD_PInstance_ID();

		// Create selection for PInstance and make sure we're enqueuing something
		final int selectionCount = queryBL.createQueryBuilder(I_M_ShipperTransportation.class, this)
				.addOnlyActiveRecordsFilter()
				.filter(filter)
				.addEqualsFilter(I_M_ShipperTransportation.COLUMN_DocStatus, IDocument.STATUS_Completed)
				.create()
				.createSelection(pInstanceId);

		Check.errorIf(selectionCount <= 0, "No record matches the process info's selection filter; AD_PInstance_ID={}, filter={}", pInstanceId, filter);
	}

	@Override
	protected String doIt() throws Exception
	{
		queryBL
				.createQueryBuilder(I_M_ShipperTransportation.class)
				.setOnlySelection(getAD_PInstance_ID())
				.create()
				.iterateAndStream()
				.filter(this::isCompleted)
				.filter(this::hasDerKurierMailAddress)
				.map(I_M_ShipperTransportation::getM_ShipperTransportation_ID)
				.map(ShipperTransportationId::ofRepoId)
				.forEach(derKurierDeliveryOrderEmailer::sendShipperTransportationAsEmail);

		return MSG_OK;
	}

	private boolean isCompleted(@NonNull final I_M_ShipperTransportation shipperTransportationRecord)
	{
		return documentBL.isDocumentCompleted(shipperTransportationRecord);
	}

	private boolean hasDerKurierMailAddress(@NonNull final I_M_ShipperTransportation shipperTransportationRecord)
	{
		final DerKurierShipperConfigRepository derKurierShipperConfigRepository = Adempiere.getBean(DerKurierShipperConfigRepository.class);
		final int shipperId = shipperTransportationRecord.getM_Shipper_ID();

		final DerKurierShipperConfig config = derKurierShipperConfigRepository.retrieveConfigForShipperIdOrNull(shipperId);
		if (config == null)
		{
			return false;
		}
		final String mailTo = config.getDeliveryOrderRecipientEmailOrNull();
		return !Check.isEmpty(mailTo, true);
	}
}
