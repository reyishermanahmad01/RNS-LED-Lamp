package com.rns.controller

import com.rns.controller.data.security.RnsSecurityCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RnsSecurityTest {

    @Test
    fun pinHashingUsesSaltAndVerifiesConstantTime() {
        val saltA = RnsSecurityCrypto.generateSaltHex()
        val saltB = RnsSecurityCrypto.generateSaltHex()
        val hashA = RnsSecurityCrypto.sha256Hex("2580", saltA)
        val hashB = RnsSecurityCrypto.sha256Hex("2580", saltB)

        assertNotEquals(hashA, hashB)
        assertTrue(RnsSecurityCrypto.verifySecret("2580", saltA, hashA))
        assertFalse(RnsSecurityCrypto.verifySecret("1234", saltA, hashA))
        assertFalse(RnsSecurityCrypto.verifySecret("", saltA, hashA))
    }

    @Test
    fun recoveryKeyAndAuthTokenFormatAreValid() {
        val rec = RnsSecurityCrypto.generateRecoveryKey()
        assertTrue(rec.startsWith("RNS-REC-"))
        assertEquals(17, rec.length)

        val tok = RnsSecurityCrypto.generateAuthToken()
        assertEquals(32, tok.length)
        assertTrue(tok.all { it in '0'..'9' || it in 'A'..'F' })
    }
}
