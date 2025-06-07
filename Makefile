# Compilador Java
JAVAC = javac

# Diretório onde estão os ficheiros fonte
SRC_DIR = src
RES_DIR = resources
LIB_DIR = lib

# Listar todos os ficheiros .java dentro de src/
SOURCES = $(shell find $(SRC_DIR) -name "*.java")

# Inclui todos os .jar do lib/ no classpath
space := $(empty) $(empty)
JARS = $(wildcard $(LIB_DIR)/*.jar)
JARS_CP = $(subst $(space),:,$(JARS))

JAVAC_FLAGS = -d $(SRC_DIR) -sourcepath $(SRC_DIR) -cp "$(JARS_CP)"
JAVA_FLAGS = -cp "$(SRC_DIR):$(JARS_CP):$(RES_DIR)"

# Nome da classe principal do programa
MAIN_CLASS = Main

# Compilar todas as classes
all:
	$(JAVAC) $(JAVAC_FLAGS) $(SOURCES)

# Limpar arquivos compilados
clean:
	find $(SRC_DIR) -name "*.class" -delete

# Executar o programa
run: all
	java $(JAVA_FLAGS) $(MAIN_CLASS)

.PHONY: all clean run