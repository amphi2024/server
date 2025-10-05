package com.amphi.server.services.trash

interface TrashService {
  fun notifyFileDelete(filePath: String)
}
