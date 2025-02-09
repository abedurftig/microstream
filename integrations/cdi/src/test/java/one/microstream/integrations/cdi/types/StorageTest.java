
package one.microstream.integrations.cdi.types;

/*-
 * #%L
 * microstream-integrations-cdi
 * %%
 * Copyright (C) 2019 - 2022 MicroStream Software
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import one.microstream.integrations.cdi.types.extension.BeanManagers;
import one.microstream.integrations.cdi.types.test.CDIExtension;


@CDIExtension
public class StorageTest
{
	@Inject
	private Agenda      agenda;
	
	@Inject
	private BeanManager beanManager;
	
	@Test
	@DisplayName("Should check if it create an instance by annotation")
	public void shouldCreateInstance()
	{
		Assertions.assertNotNull(this.agenda);
	}
	
	@Test
	public void shouldCreateNameRootInjection()
	{
		this.agenda.add("Otavio");
		this.agenda.add("Ada");
		final Set<Bean<?>> beans = this.beanManager.getBeans(Agenda.class);
		Assertions.assertFalse(beans.isEmpty());
		final Agenda instance = BeanManagers.getInstance(Agenda.class);
		Assertions.assertEquals(instance, this.agenda);
		Assertions.assertEquals(instance.getNames(), this.agenda.getNames());
		
	}
}
