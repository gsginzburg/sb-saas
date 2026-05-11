/*
 * Copyright 2026 Gary Ginzburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsginzburg.cluster.framework.management;

import java.net.URI;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PgDumpService {

    /**
     * Dumps a single schema from source database to a file.
     * jdbcUrl format: jdbc:postgresql://host:port/dbname
     */
    public Path dumpSchema(String jdbcUrl, String username, String password,
                           String schemaName, Path outputFile) throws Exception {
        JdbcUrlParts parts = parseJdbcUrl(jdbcUrl);
        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", parts.host(),
                "-p", String.valueOf(parts.port()),
                "-U", username,
                "-d", parts.dbname(),
                "--schema=" + schemaName,
                "-F", "c",  // custom format
                "-f", outputFile.toString()
        );
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("pg_dump failed (exit " + exitCode + "): " + output);
        }
        log.info("pg_dump completed for schema {} -> {}", schemaName, outputFile);
        return outputFile;
    }

    /**
     * Restores a schema dump to the target database.
     */
    public void restoreSchema(String jdbcUrl, String username, String password,
                              Path dumpFile) throws Exception {
        JdbcUrlParts parts = parseJdbcUrl(jdbcUrl);
        ProcessBuilder pb = new ProcessBuilder(
                "pg_restore",
                "-h", parts.host(),
                "-p", String.valueOf(parts.port()),
                "-U", username,
                "-d", parts.dbname(),
                "-F", "c",
                dumpFile.toString()
        );
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("pg_restore failed (exit " + exitCode + "): " + output);
        }
        log.info("pg_restore completed from {}", dumpFile);
    }

    private JdbcUrlParts parseJdbcUrl(String jdbcUrl) {
        // jdbc:postgresql://host:port/dbname
        String stripped = jdbcUrl.replace("jdbc:", "");
        URI uri = URI.create(stripped);
        return new JdbcUrlParts(
                uri.getHost(),
                uri.getPort() > 0 ? uri.getPort() : 5432,
                uri.getPath().substring(1) // remove leading /
        );
    }

    record JdbcUrlParts(String host, int port, String dbname) {}
}
