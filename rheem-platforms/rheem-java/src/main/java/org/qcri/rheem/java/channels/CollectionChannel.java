package org.qcri.rheem.java.channels;

import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.OptimizationContext;
import org.qcri.rheem.core.plan.executionplan.Channel;
import org.qcri.rheem.core.plan.rheemplan.OutputSlot;
import org.qcri.rheem.core.platform.ChannelDescriptor;
import org.qcri.rheem.core.util.Tuple;
import org.qcri.rheem.java.operators.JavaExecutionOperator;

import java.util.Collection;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link Channel} between two {@link JavaExecutionOperator}s using an intermediate {@link Collection}.
 */
public class CollectionChannel extends Channel {

    private static final boolean IS_REUSABLE = true;

    private static final boolean IS_INTERNAL = true;

    public static final ChannelDescriptor DESCRIPTOR = new ChannelDescriptor(CollectionChannel.class, IS_REUSABLE, IS_REUSABLE, !IS_INTERNAL && IS_REUSABLE);

    protected CollectionChannel(ChannelDescriptor channelDescriptor, OutputSlot<?> outputSlot) {
        super(channelDescriptor, outputSlot);
        assert channelDescriptor == DESCRIPTOR;
    }

    private CollectionChannel(CollectionChannel parent) {
        super(parent);
    }

    @Override
    public CollectionChannel copy() {
        return new CollectionChannel(this);
    }

    public CollectionChannel.Executor createExecutor() {
        return new Executor(this.isMarkedForInstrumentation());
    }

    /**
     * {@link JavaChannelInitializer} implementation for the {@link CollectionChannel}.
     */
    public static class Initializer implements JavaChannelInitializer {

        @Override
        public StreamChannel provideStreamChannel(Channel channel, OptimizationContext optimizationContext) {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public Tuple<Channel, Channel> setUpOutput(ChannelDescriptor descriptor, OutputSlot<?> outputSlot, OptimizationContext optimizationContext) {
            assert descriptor == CollectionChannel.DESCRIPTOR;
            // TODO: We could add a "Collector" operator in between.
            final CollectionChannel collectionChannel = new CollectionChannel(descriptor, outputSlot);
            return new Tuple<>(collectionChannel, collectionChannel);
        }

        @Override
        public Channel setUpOutput(ChannelDescriptor descriptor, Channel source, OptimizationContext optimizationContext) {
            throw new UnsupportedOperationException("Not yet implemented.");
        }
    }

    /**
     * {@link ChannelExecutor} implementation for the {@link CollectionChannel}.
     */
    public class Executor implements ChannelExecutor {

        private Collection<?> collection;

        private long cardinality = -1;

        private boolean isMarkedForInstrumentation;

        public Executor(boolean isMarkedForInstrumentation) {
            this.isMarkedForInstrumentation = isMarkedForInstrumentation;
        }

        @Override
        public void acceptStream(Stream<?> stream) {
            this.collection = stream.collect(Collectors.toList());
            this.cardinality = this.collection.size();
        }

        @Override
        public void acceptCollection(Collection<?> collection) {
            this.collection = collection;
            this.cardinality = this.collection.size();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Collection<?> provideCollection() {
            return this.collection;
        }

        @Override
        public boolean canProvideCollection() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Stream<?> provideStream() {
            return this.collection.stream();
        }

        @Override
        public void markForInstrumentation() {
            this.isMarkedForInstrumentation = true;
        }

        @Override
        public boolean ensureExecution() {
            assert this.collection != null : String.format("Could not ensure execution of %s.", this.getChannel());
            return true;
        }

        @Override
        public Channel getChannel() {
            return CollectionChannel.this;
        }

        @Override
        public OptionalLong getMeasuredCardinality() {
            return this.cardinality == -1 ? OptionalLong.empty() : OptionalLong.of(this.cardinality);
        }

        @Override
        public void release() throws RheemException {
            this.collection = null;
        }
    }
}