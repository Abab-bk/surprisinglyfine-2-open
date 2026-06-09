package com.rorokaiiworks.goodidlegame

class AndroidDLCService : DLCService {
    override fun unlocked(dlc: DLC): Boolean {
        return false
    }

    override fun enabled(dlc: DLC): Boolean {
        return false
    }

    override fun goToDlcShop(dlc: DLC): Boolean {
        return false
    }
}