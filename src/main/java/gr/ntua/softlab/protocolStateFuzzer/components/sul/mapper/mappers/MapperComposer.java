package gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.AbstractOutputChecker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MapperComposer implements Mapper {
	private static final Logger LOGGER = LogManager.getLogger(MapperComposer.class);
	protected InputMapper inputMapper;
	protected OutputMapper outputMapper;

	protected AbstractOutputChecker abstractOutputChecker;

	public MapperComposer(InputMapper inputMapper, OutputMapper outputMapper,
						  AbstractOutputChecker abstractOutputChecker) {
		this.inputMapper = inputMapper;
		this.outputMapper = outputMapper;
		this.abstractOutputChecker = abstractOutputChecker;
	}

	public MapperConfig getMapperConfig(){
		return outputMapper.getMapperConfig();
	}

	public AbstractOutputChecker getAbstractOutputChecker() {
		return abstractOutputChecker;
	}

	@Override
	public AbstractOutput execute(AbstractInput input, ExecutionContext context) {
		LOGGER.info("Executing input symbol {}", input.getName());
		AbstractOutput output;
		context.setInput(input);
		if (context.isExecutionEnabled() && input.isEnabled(context)) {
			output = doExecute(input, context);
		} else {
			output = outputMapper.disabled(); 
		}
		LOGGER.info("Produced output symbol {}", output.getName());
		return output;
	}

	protected AbstractOutput doExecute(AbstractInput input, ExecutionContext context) {
		inputMapper.sendInput(input, context);
		AbstractOutput output = outputMapper.receiveOutput(context);
		inputMapper.postReceive(input, output, context);
		return output;
	}
}
