# R8 shrinks, optimises and obfuscates the release build.
#
# There is no keep rule here because there is nothing to keep. Every library the app ships
# publishes its own consumer rules, and no app class is reached by reflection or by name.
