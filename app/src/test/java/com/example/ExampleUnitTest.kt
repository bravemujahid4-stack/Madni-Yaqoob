package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testSupplierPartyAccountType() {
    val supplierType = PartyAccountType.Supplier
    assertEquals("SUP", supplierType.codePrefix)
    assertEquals("Supplier", supplierType.displayName)
    assertEquals("Accounts Payable", supplierType.defaultGlType)
    assertEquals("Credit", supplierType.defaultNature)
  }

  @Test
  fun testSoftDeleteAndRestorePartyAccount() {
    val initialDeletedCount = MasRepository.deletedRecords.value.size
    val party = PartyAccount(
      id = "TEST-SUP-999",
      code = "SUP-999",
      name = "Test Metal Scrap Vendor",
      accountType = PartyAccountType.Supplier,
      openingBalance = 75000.0,
      balanceType = "Credit",
      phone = "+92 300 9999999",
      address = "Plot 42, Steel Market",
      notes = "Primary scrap provider"
    )

    // Save
    val saved = MasRepository.savePartyAccount(party, updateExisting = true)
    assertTrue(saved)
    assertTrue(MasRepository.partyAccounts.value.any { it.id == "TEST-SUP-999" })

    // Soft delete
    val deleted = MasRepository.deletePartyAccount("TEST-SUP-999")
    assertTrue(deleted)
    assertFalse(MasRepository.partyAccounts.value.any { it.id == "TEST-SUP-999" })
    assertEquals(initialDeletedCount + 1, MasRepository.deletedRecords.value.size)

    val trashItem = MasRepository.deletedRecords.value.first()
    assertEquals("Party Account", trashItem.itemType)
    assertEquals("SUP-999", trashItem.itemCode)
    assertEquals("Test Metal Scrap Vendor", trashItem.title)

    // Restore
    val restored = MasRepository.restoreDeletedRecord(trashItem.id)
    assertTrue(restored)
    assertTrue(MasRepository.partyAccounts.value.any { it.id == "TEST-SUP-999" })
    assertEquals(initialDeletedCount, MasRepository.deletedRecords.value.size)
  }

  @Test
  fun testCashInHandLedgerCalculation() {
    val munawarCash = MasRepository.accounts.value.find { it.name.contains("Munawar", ignoreCase = true) }
    assertNotNull(munawarCash)
    val balance = MasRepository.getAccountLedgerBalance(munawarCash!!)
    assertNotNull(balance.nature)
    assertTrue(balance.balance >= 0.0)
  }
}

