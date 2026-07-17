#
# Module to add jdbc drivers to classpath
#
# Drivers are staged by perc-distribution-tree into jetty.base/lib/jdbc/
# (see installDistributionFiles.xml), not into jetty.home or defaults.
#
# [lib] patterns are resolved by BaseHome.getPaths() as relative paths
# against each config source (jetty.base, jetty.home, --include-jetty-dir).
# Do NOT use the basehome: URI scheme here — that is only for [files]
# entries (FileArg / BaseHomeFileInitializer). PathMatchers treats
# "basehome:lib/jdbc/*.jar" as a literal path segment:
#   - Windows: Path.of throws InvalidPathException (illegal ':') — issue #1179
#   - Unix:    looks for jetty.base/basehome:lib/jdbc/ which does not exist
#
# Align with perc.mod [lib] lib/jdbc/**.jar (relative, base-first search).
#

[depends]
perc-config

[lib]
lib/jdbc/**.jar

[xml]
etc/perc-ds.properties
etc/perc-ds.xml

[ini]


