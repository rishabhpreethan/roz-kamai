package com.viis.rozkamai.data.repository

import com.viis.rozkamai.data.local.dao.EventDao
import com.viis.rozkamai.data.local.entity.EventEntity
import javax.inject.Inject

/**
 * Delegates event persistence to [EventDao].
 * Events are append-only — never updated or deleted.
 */
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
) : EventRepository {

    override suspend fun appendEvent(event: EventEntity) {
        eventDao.appendEvent(event)
    }
}
