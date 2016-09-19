package net.filebot.ui;

import static org.junit.Assert.*;

import java.util.stream.IntStream;

import org.junit.Test;

public class SupportDialogTest {

	@Test
	public void feelingLucky() {
		assertTrue(SupportDialog.AppStoreReview.feelingLucky(2000, 2000, 500, 400, 0));
		assertFalse(SupportDialog.AppStoreReview.feelingLucky(2000, 2000, 400, 400, 0));

		assertTrue(SupportDialog.Donation.feelingLucky(2000, 2000, 400, 400, 0));
		assertFalse(SupportDialog.Donation.feelingLucky(100, 100, 400, 400, 0));

		assertTrue(IntStream.range(0, 100).anyMatch(i -> SupportDialog.Donation.feelingLucky(0, 5000, 400, 400, 0)));
		assertFalse(IntStream.range(0, 100).anyMatch(i -> SupportDialog.Donation.feelingLucky(0, 5000, 400, 400, 2)));
	}

}
