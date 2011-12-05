package org.eclipse.egit.ui.wizards.clone;

import org.junit.BeforeClass;
import org.junit.Test;

public class GitImportWizardTest extends GitCloneWizardTestBase {

	@BeforeClass
	public static void setup() throws Exception {
		r = new SampleTestRepository(NUMBER_RANDOM_COMMITS, false);
	}

	@Test
	public void updatesParameterFieldsInImportDialogWhenURIIsUpdated()
			throws Exception {

		importWizard.openWizard2();
	}
}
