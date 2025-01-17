package org.interledger.spsp.server.config.ilp;

import org.interledger.connector.ccp.codecs.CcpCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.spsp.stream.ConfigurableSpspStreamConnectionGenerator;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.receiver.StreamConnectionGenerator;
import org.interledger.stream.receiver.StreamReceiver;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamConfig {

  public static final String STREAM = "STREAM";

  @Bean
  @Qualifier(STREAM)
  public CodecContext streamCodecContext() {
    return CcpCodecContextFactory.oer();
  }

  @Bean
  protected StreamEncryptionService streamEncryptionService() {
    return new JavaxStreamEncryptionService();
  }

  @Bean
  protected StreamConnectionGenerator streamConnectionGenerator() {
    return new ConfigurableSpspStreamConnectionGenerator();
  }

  @Bean
  protected StreamReceiver streamReceiver(
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator,
    final StreamEncryptionService streamEncryptionService,
    final CodecContext streamCodecContext
  ) {
    return new StatelessStreamReceiver(
      serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, streamCodecContext
    );
  }

}
