package de.metas.report.jasper.client.interceptor;

import org.adempiere.ad.callout.annotations.Callout;
import org.adempiere.ad.callout.annotations.CalloutMethod;
import org.adempiere.ad.callout.spi.IProgramaticCalloutProvider;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_AD_Process;
import org.springframework.stereotype.Component;

import de.metas.report.jasper.client.process.JasperReportStarter;
import lombok.NonNull;

/*
 * #%L
 * de.metas.report.jasper.client
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

@Callout(I_AD_Process.class)
@Component
public class AD_Process
{
	public AD_Process()
	{
		Services
				.get(IProgramaticCalloutProvider.class)
				.registerAnnotatedCallout(this);
	}

	@CalloutMethod(columnNames = I_AD_Process.COLUMNNAME_IsReport)
	public void setDefaultReportProcessClassName(@NonNull final I_AD_Process processRecord)
	{
		if (processRecord.isReport() && Check.isEmpty(processRecord.getClassname(), true))
		{
			processRecord.setClassname(JasperReportStarter.class.getName());
		}
	}
}
