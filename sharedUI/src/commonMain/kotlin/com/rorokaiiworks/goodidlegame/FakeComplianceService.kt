package com.rorokaiiworks.goodidlegame

class FakeComplianceService : ComplianceService {
    override suspend fun startCompliance(): Boolean {
        return true
    }
}