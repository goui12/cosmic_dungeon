package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;

public class ProtocolInfoBuilder<T extends PacketListener, B extends ByteBuf, C> {
    final ConnectionProtocol protocol;
    final PacketFlow flow;
    private final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs = new ArrayList<>();
    @Nullable
    private BundlerInfo bundlerInfo;

    public ProtocolInfoBuilder(ConnectionProtocol protocol, PacketFlow flow) {
        this.protocol = protocol;
        this.flow = flow;
    }

    public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(PacketType<P> type, StreamCodec<? super B, P> serializer) {
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(type, serializer, null));
        return this;
    }

    public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(
        PacketType<P> type, StreamCodec<? super B, P> serializer, CodecModifier<B, P, C> modifier
    ) {
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(type, serializer, modifier));
        return this;
    }

    public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B, C> withBundlePacket(
        PacketType<P> type, Function<Iterable<Packet<? super T>>, P> bundler, D packet
    ) {
        StreamCodec<ByteBuf, D> streamcodec = StreamCodec.unit(packet);
        PacketType<D> packettype = (PacketType<D>)(PacketType<?>)packet.type();
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(packettype, streamcodec, null));
        this.bundlerInfo = BundlerInfo.createForPacket(type, bundler, packet);
        return this;
    }

    StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(
        Function<ByteBuf, B> bufferFactory, List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> entries, C context
    ) {
        ProtocolCodecBuilder<ByteBuf, T> protocolcodecbuilder = new ProtocolCodecBuilder<>(this.flow);

        for (ProtocolInfoBuilder.CodecEntry<T, ?, B, C> codecentry : entries) {
            codecentry.addToBuilder(protocolcodecbuilder, bufferFactory, context);
        }

        return protocolcodecbuilder.build();
    }

    private static ProtocolInfo.Details buildDetails(
        final ConnectionProtocol protocol, final PacketFlow flow, final List<? extends ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>> entries
    ) {
        return new ProtocolInfo.Details() {
            @Override
            public ConnectionProtocol id() {
                return protocol;
            }

            @Override
            public PacketFlow flow() {
                return flow;
            }

            @Override
            public void listPackets(ProtocolInfo.Details.PacketVisitor p_412067_) {
                for (int i = 0; i < entries.size(); i++) {
                    ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?> codecentry = (ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>)entries.get(i);
                    p_412067_.accept(codecentry.type, i);
                }
            }
        };
    }

    public SimpleUnboundProtocol<T, B> buildUnbound(final C context) {
        final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
        final BundlerInfo bundlerinfo = this.bundlerInfo;
        final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
        return new SimpleUnboundProtocol<T, B>() {
            @Override
            public ProtocolInfo<T> bind(Function<ByteBuf, B> p_412690_) {
                return new ProtocolInfoBuilder.Implementation<>(
                    ProtocolInfoBuilder.this.protocol,
                    ProtocolInfoBuilder.this.flow,
                    ProtocolInfoBuilder.this.buildPacketCodec(p_412690_, list, context),
                    bundlerinfo
                );
            }

            @Override
            public ProtocolInfo.Details details() {
                return protocolinfo$details;
            }
        };
    }

    public UnboundProtocol<T, B, C> buildUnbound() {
        final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
        final BundlerInfo bundlerinfo = this.bundlerInfo;
        final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
        return new UnboundProtocol<T, B, C>() {
            @Override
            public ProtocolInfo<T> bind(Function<ByteBuf, B> p_412297_, C p_412114_) {
                return new ProtocolInfoBuilder.Implementation<>(
                    ProtocolInfoBuilder.this.protocol,
                    ProtocolInfoBuilder.this.flow,
                    ProtocolInfoBuilder.this.buildPacketCodec(p_412297_, list, p_412114_),
                    bundlerinfo
                );
            }

            @Override
            public ProtocolInfo.Details details() {
                return protocolinfo$details;
            }
        };
    }

    private static <L extends PacketListener, B extends ByteBuf> SimpleUnboundProtocol<L, B> protocol(
        ConnectionProtocol protocol, PacketFlow flow, Consumer<ProtocolInfoBuilder<L, B, Unit>> packetAdder
    ) {
        ProtocolInfoBuilder<L, B, Unit> protocolinfobuilder = new ProtocolInfoBuilder<>(protocol, flow);
        packetAdder.accept(protocolinfobuilder);
        return protocolinfobuilder.buildUnbound(Unit.INSTANCE);
    }

    public static <T extends ServerboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> serverboundProtocol(
        ConnectionProtocol p_protocol, Consumer<ProtocolInfoBuilder<T, B, Unit>> packetAdder
    ) {
        return protocol(p_protocol, PacketFlow.SERVERBOUND, packetAdder);
    }

    public static <T extends ClientboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> clientboundProtocol(
        ConnectionProtocol p_protocol, Consumer<ProtocolInfoBuilder<T, B, Unit>> packetAdder
    ) {
        return protocol(p_protocol, PacketFlow.CLIENTBOUND, packetAdder);
    }

    private static <L extends PacketListener, B extends ByteBuf, C> UnboundProtocol<L, B, C> contextProtocol(
        ConnectionProtocol protocol, PacketFlow flow, Consumer<ProtocolInfoBuilder<L, B, C>> packetAdder
    ) {
        ProtocolInfoBuilder<L, B, C> protocolinfobuilder = new ProtocolInfoBuilder<>(protocol, flow);
        packetAdder.accept(protocolinfobuilder);
        return protocolinfobuilder.buildUnbound();
    }

    public static <T extends ServerboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextServerboundProtocol(
        ConnectionProtocol protocol, Consumer<ProtocolInfoBuilder<T, B, C>> packetAdder
    ) {
        return contextProtocol(protocol, PacketFlow.SERVERBOUND, packetAdder);
    }

    public static <T extends ClientboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextClientboundProtocol(
        ConnectionProtocol protocol, Consumer<ProtocolInfoBuilder<T, B, C>> packetAdder
    ) {
        return contextProtocol(protocol, PacketFlow.CLIENTBOUND, packetAdder);
    }

    record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf, C>(
        PacketType<P> type, StreamCodec<? super B, P> serializer, @Nullable CodecModifier<B, P, C> modifier
    ) {
        public void addToBuilder(ProtocolCodecBuilder<ByteBuf, T> builder, Function<ByteBuf, B> bufferFactory, C context) {
            StreamCodec<? super B, P> streamcodec;
            if (this.modifier != null) {
                streamcodec = this.modifier.apply(this.serializer, context);
            } else {
                streamcodec = this.serializer;
            }

            StreamCodec<ByteBuf, P> streamcodec1 = streamcodec.mapStream(bufferFactory);
            builder.add(this.type, streamcodec1);
        }
    }

    record Implementation<L extends PacketListener>(
        ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo
    ) implements ProtocolInfo<L> {
    }
}
