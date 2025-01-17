package org.interledger.spsp.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.SharedSecret;
import org.interledger.stream.Denomination;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.google.common.io.BaseEncoding;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Unit tests for {@link ConfigurableSpspStreamConnectionGenerator}.
 */
public class ConfigurableSpspStreamConnectionGeneratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void fulfillsPacketsSentToJavascriptReceiver() throws IOException {

    // This was created by the JS ilp-protocol-stream library
    //let ilp_address = Address::from_str("test.peerB").unwrap();
    final InterledgerAddress ilpAddress = InterledgerAddress.of("test.peerB");

    // This prepare packet was taken from the JS implementation
    final byte[] bytes = BaseEncoding.base16().decode(
      "0C819900000000000001F43230313931303238323134313533383338F31A96346C613011947F39A0F1F4E573C2FC3E7E53797672B01D28"
        + "98E90C9A0723746573742E70656572422E4E6A584430754A504275477A353653426D4933755836682D3B6CC484C0D4E9282275D4B37"
        + "C6AE18F35B497DDBFCBCE6D9305B9451B4395C3158AA75E05BF27582A237109EC6CA0129D840DA7ABD96826C8147D0D"
    );

    final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    final InterledgerPreparePacket prepare = StreamCodecContextFactory.oer().read(InterledgerPreparePacket.class, bais);

    final InterledgerCondition executionCondition = prepare.getExecutionCondition();
    final byte[] serverSecret = new byte[32]; // All 0's
    final ServerSecretSupplier serverSecretSupplier = () -> serverSecret;

    final StreamConnectionGenerator connectionGenerator = new ConfigurableSpspStreamConnectionGenerator();
    final SharedSecret sharedSecret = connectionGenerator
      .deriveSecretFromAddress(() -> serverSecret, prepare.getDestination());

    assertThat(BaseEncoding.base16().encode(sharedSecret.key()))
      .withFailMessage(" Did not regenerate the same shared secret")
      .isEqualTo("B7D09D2E16E6F83C55B60E42FCD7C2B8ED49624A1DF73C59B383DBE2E8690309");

    final JavaxStreamEncryptionService streamEncryptionService = new JavaxStreamEncryptionService();
    final StatelessStreamReceiver streamReceiver = new StatelessStreamReceiver(
      serverSecretSupplier, connectionGenerator, streamEncryptionService,
      StreamCodecContextFactory.oer()
    );

    final Denomination denomination = Denomination.builder()
      .assetScale((short) 9)
      .assetCode("ABC")
      .build();

    streamReceiver.receiveMoney(prepare, ilpAddress, denomination).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment().validateCondition(executionCondition))
        .withFailMessage("fulfillment generated does not hash to the expected condition")
        .isTrue(),
      rejectPacket -> fail()
    );
  }

}
