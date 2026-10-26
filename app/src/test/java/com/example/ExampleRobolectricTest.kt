package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ContactRepository
import com.example.model.DeviceContact
import com.example.model.P2PPacket
import com.example.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GhostLink", appName)
  }

  @Test
  fun `test p2p packet serialization and deserialization`() {
    val packet = P2PPacket(
      type = PacketType.CHAT,
      senderName = "Peer1",
      senderIp = "192.168.1.100",
      content = "Hello local mesh!\nLine 2"
    )
    val serialized = packet.serialize()
    val deserialized = P2PPacket.deserialize(serialized)

    assertNotNull(deserialized)
    assertEquals(PacketType.CHAT, deserialized?.type)
    assertEquals("Peer1", deserialized?.senderName)
    assertEquals("192.168.1.100", deserialized?.senderIp)
    assertEquals("Hello local mesh!\nLine 2", deserialized?.content)
  }

  @Test
  fun `test contact repository save and resolve`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ContactRepository(context)

    repo.saveContact("Living Room TV", "192.168.1.55", "Living room node")
    val found = repo.findByIp("192.168.1.55")

    assertNotNull(found)
    assertEquals("Living Room TV", found?.name)
    assertEquals("192.168.1.55", found?.ip)
    assertEquals("Living room node", found?.note)
  }
}
