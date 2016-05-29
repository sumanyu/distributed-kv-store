package core.api

import java.util.concurrent.ConcurrentHashMap

trait KVStore[Key, Value] {
  def get(key: Key): Value
  def put(key: Key, value: Value): Value
  def delete(key: Key): Unit
  def contains(key: Key): Boolean
}

class HashInMemoryKVStore[Key, Value] extends KVStore[Key, Value] {

  private val store = new ConcurrentHashMap[Key, Value]()

  def get(key: Key): Value = store.get(key)
  def put(key: Key, value: Value): Value = store.put(key, value)
  def delete(key: Key): Unit = store.remove(key)
  def contains(key: Key): Boolean = store.contains(key)
}

