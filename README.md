# JEAF Xpand Eclipse

Eclipse editors for the template language Xpand, the extension language Xtend and the constraint language Check, as used with [JEAF Generator](https://www.jeaf-generator.io).

The code is based on [Eclipse M2T Xpand](https://projects.eclipse.org/projects/modeling.m2t.xpand) 2.2.0 (repository [eclipse/xpand/org.eclipse.xpand](https://gitlab.eclipse.org/eclipse/xpand/org.eclipse.xpand), commit `773b6cadc8c3ff4d6a2356726093b93fb0a4fe2c`), which is not maintained anymore. The editors of Eclipse Xpand 2.2.0 can't be installed in current Eclipse versions.

Update site: `https://www.jeaf-generator.io/distribution/xpand`

The update site contains all released versions. New versions are offered by "Check for Updates" in Eclipse.

## Content

| Feature | Content |
| ------- | ------- |
| `org.eclipse.xpand.ui`               | Xpand editor (incl. Xtend and Check editors) |
| `org.eclipse.xtend.ui`               | Xtend and Check editors |
| `org.eclipse.xtend.typesystem.emf`   | EMF type system incl. UI |
| `org.eclipse.xtend.typesystem.uml2`  | UML2 type system incl. UI |
| `org.eclipse.xpand`, `org.eclipse.xtend` | Runtime of Xpand / Xtend |

Plug-ins and features keep the IDs of Eclipse Xpand, so existing projects and settings (e.g. project natures, metamodel contributors) work without changes.

## Differences to Eclipse Xpand 2.2.0

- Version 2.3.0 of the UI plug-ins and of the features. The runtime plug-ins (`org.eclipse.xpand`, `org.eclipse.xtend`, ...) are the unchanged ones of Eclipse Xpand 2.2.0.
- Requires Java 17 and Eclipse 2024-09 or newer.
- ICU4J is replaced by `java.text`, as it is no longer part of Eclipse.
- "Show in" of the search view uses the Project Explorer instead of the removed Resource Navigator.
- Feature `org.eclipse.xtend` contains ANTLR runtime 3.2 instead of feature `org.eclipse.xtend.dependencies` (incl. log4j 1.2).
- Features no longer declare required plug-ins, as some of them don't exist anymore under their old names (e.g. `org.apache.commons.logging`). Dependencies are defined by the plug-ins only.
- Fix of `ConcurrentModificationException` in `XtendXpandProject.analyze(...)`, e.g. when changing metamodel contributors.
- No internal Eclipse APIs are used anymore (JDT, search, preference pages).
- Key binding Alt+Shift+T (refactoring quick menu) works in the Xpand editor.
- No project wizard (File > New > Xpand Project). Templates of JEAF Generator are maintained in Maven projects; the Xpand nature can be added in the project's context menu (Configure > Add Xpand Nature). Wizards for Xpand, Xtend and Check files are still available.
- Context menus are contributed as commands (no deprecated action contributions anymore). No dependency on Guava.
- Xpand resources in archives (e.g. templates in JAR files) don't keep the archive open, so archives aren't locked on Windows.
- Default metamodel contributors for templates of JEAF Generator: "UML2 profiles" and "oAW-Classic Metamodel".
- Features refer to the update site above, so Eclipse knows it for updates.
- All artifacts of the update site are PGP signed with the JEAF release key.

## Build

```
mvn clean verify
```

Tests in `tests/` run in an Eclipse workbench, which opens a window during the build. On Linux without display, use `xvfb-run -a mvn clean verify` (as the GitHub workflows do).

The update site is created in `site/target/repository`. Plug-ins are built with [Tycho](https://github.com/eclipse-tycho/tycho) against Eclipse 2024-09 and the update site of Eclipse Xpand 2.2.0 (see `jeaf-xpand-eclipse.target`).

## Publishing a new version

The update site is a composite p2 repository in [jeaf-generator.io](https://github.com/anaptecs/jeaf-generator.io) (`distribution/xpand`) with one child repository per version.

1. Run workflow "Maven Release Build" with the current version without `-SNAPSHOT` (e.g. `2.3.0`) and tag (e.g. `v2.3.0`). It builds the update site with PGP signatures (profile `JEAFRelease`), tags the release and provides the update site as workflow artifact.
2. Copy the workflow artifact to `distribution/xpand/<version>` (e.g. `gh run download <run-id> -n jeaf-xpand-eclipse-<version> -D distribution/xpand/<version>`).
3. Add `<child location='<version>'/>` to `compositeContent.xml` and `compositeArtifacts.xml` and increase `size` of `children`.
4. Increase the version for the next release (e.g. `mvn tycho-versions:set-version -DnewVersion=2.4.0-SNAPSHOT`).

## License

Eclipse Public License 1.0 (see [LICENSE](LICENSE)), as Eclipse Xpand itself.
