/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonschema.main.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.google.common.collect.Lists;
import joptsimple.HelpFormatter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.github.fge.jsonschema.main.cli.RetCode.*;

public final class Main
{
    private static final HelpFormatter HELP = new CustomHelpFormatter();

    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();

    private final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    private final SyntaxValidator syntaxValidator
        = factory.getSyntaxValidator();

    public static void main(final String... args)
        throws IOException, ProcessingException
    {
        final OptionParser parser = new OptionParser();
        parser.accepts("syntax",
            "check the syntax of schema(s) given as argument(s)");
        parser.accepts("brief", "only show validation status (OK/NOT OK)");
        parser.accepts("help", "show this help").forHelp();
        parser.formatHelpWith(HELP);

        final OptionSet optionSet;
        final boolean isSyntax;
        final int requiredArgs;
        Reporter reporter = Reporters.DEFAULT;

        try {
            optionSet = parser.parse(args);
            if (optionSet.has("help")) {
                parser.printHelpOn(System.out);
                System.exit(ALL_OK.get());
            }
        } catch (OptionException e) {
            System.err.println("unrecognized option(s): "
                + CustomHelpFormatter.OPTIONS_JOINER.join(e.options()));
            parser.printHelpOn(System.err);
            System.exit(CMD_ERROR.get());
            throw new IllegalStateException("WTF??");
        }

        isSyntax = optionSet.has("syntax");
        requiredArgs = isSyntax ? 1 : 2;

        @SuppressWarnings("unchecked")
        final List<String> arguments
            = (List<String>) optionSet.nonOptionArguments();

        if (arguments.size() < requiredArgs) {
            System.err.println("missing arguments");
            parser.printHelpOn(System.err);
            System.exit(CMD_ERROR.get());
        }

        final List<File> files = Lists.newArrayList();
        for (final String target: arguments)
            files.add(new File(target).getCanonicalFile());

        if (optionSet.has("brief"))
            reporter = Reporters.BRIEF;

        new Main().proceed(reporter, isSyntax, files);
    }

    private void proceed(final Reporter reporter, final boolean isSyntax,
        final List<File> files)
        throws IOException, ProcessingException
    {
        final RetCode retCode = isSyntax ? doSyntax(reporter, files)
            : doValidation(reporter, files);
        System.exit(retCode.get());
    }

    private RetCode doSyntax(final Reporter reporter, final List<File> files)
        throws IOException
    {
        RetCode retcode, ret = ALL_OK;
        String fileName;
        JsonNode node;

        for (final File file: files) {
            fileName = file.toString();
            node = MAPPER.readTree(file);
            retcode = reporter.validateSchema(syntaxValidator, fileName, node);
            if (retcode != ALL_OK)
                ret = retcode;
        }

        return ret;
    }

    private RetCode doValidation(final Reporter reporter,
        final List<File> files)
        throws IOException, ProcessingException
    {
        final File schemaFile = files.remove(0);
        JsonNode node;

        node = MAPPER.readTree(schemaFile);
        if (!syntaxValidator.schemaIsValid(node)) {
            System.err.println("Schema is invalid! Aborting...");
            return SCHEMA_SYNTAX_ERROR;
        }

        final JsonSchema schema = factory.getJsonSchema(node);

        RetCode ret = ALL_OK, retcode;

        for (final File file: files) {
            node = MAPPER.readTree(file);
            retcode = reporter.validateInstance(schema, file.toString(), node);
            if (retcode != ALL_OK)
                ret = retcode;
        }

        return ret;
    }
}
