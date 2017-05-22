package net.filebot.ui;

import static org.junit.Assert.*;

import java.util.stream.IntStream;

import org.junit.Test;

public class SupportDialogTest {

	@Test
	public void feelingLucky() {
		assertTrue(SupportDialog.Donation.feelingLucky(2000, 2000, 400, -1, 0));

		// make sure that it happens sometimes or never
		assertTrue(IntStream.range(0, 100).anyMatch(i -> SupportDialog.AppStoreReview.feelingLucky(6001, 5001, 500, -1, 0)));
		assertTrue(IntStream.range(0, 100).noneMatch(i -> SupportDialog.AppStoreReview.feelingLucky(2000, 2000, 400, 400, 1)));

		assertTrue(IntStream.range(0, 100).anyMatch(i -> SupportDialog.Donation.feelingLucky(0, 5000, 400, -1, 0)));
		assertFalse(IntStream.range(0, 100).anyMatch(i -> SupportDialog.Donation.feelingLucky(0, 5000, 400, 400, 2)));
	}

}
