# JEAF Xpand Eclipse

Eclipse editors for the template language Xpand, the extension language Xtend and the constraint language Check, as used with [JEAF Generator](https://www.jeaf-generator.io).

The code is based on [Eclipse M2T Xpand](https://projects.eclipse.org/projects/modeling.m2t.xpand) 2.2.0 (repository [eclipse/xpand/org.eclipse.xpand](https://gitlab.eclipse.org/eclipse/xpand/org.eclipse.xpand), commit `773b6cadc8c3ff4d6a2356726093b93fb0a4fe2c`), which is not maintained anymore. The editors of Eclipse Xpand 2.2.0 can't be installed in current Eclipse versions.

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

- Version 2.2.1 of the UI plug-ins and of the features. The runtime plug-ins (`org.eclipse.xpand`, `org.eclipse.xtend`, ...) are the unchanged ones of Eclipse Xpand 2.2.0.
- Requires Java 17 and Eclipse 2024-09 or newer.
- ICU4J is replaced by `java.text`, as it is no longer part of Eclipse.
- "Show in" of the search view uses the Project Explorer instead of the removed Resource Navigator.
- Feature `org.eclipse.xtend` contains ANTLR runtime 3.2 instead of feature `org.eclipse.xtend.dependencies` (incl. log4j 1.2).

## Build

```
mvn clean verify
```

The update site is created in `site/target/repository`. Plug-ins are built with [Tycho](https://github.com/eclipse-tycho/tycho) against Eclipse 2024-09 and the update site of Eclipse Xpand 2.2.0 (see `jeaf-xpand-eclipse.target`).

## License

Eclipse Public License 1.0 (see [LICENSE](LICENSE)), as Eclipse Xpand itself.
