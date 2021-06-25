package xyz.rgnt.revoken.common.providers.storage.flatfile;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.revoken.common.Revoken;
import xyz.rgnt.revoken.common.providers.storage.flatfile.store.AStore;


public class StorageProvider {

    private final @NotNull Revoken<?> instance;

    /**
     * Default constructor
     *
     * @param instance Instance of plugin
     */
    public StorageProvider(@NotNull Revoken<?> instance) {
        this.instance = instance;
    }

    /**
     * Constructs YAML Store, and prepares it
     *
     * @param resourceRoot Root of the resource in binary
     * @param path       Path to resource
     * @param hasDefault Load default if not available in data folder
     * @return YAML Store
     * @throws Exception When something goes wrong
     */
    public @NotNull AStore provideYaml(@Nullable String resourceRoot, @NotNull String path, boolean hasDefault) throws Exception {
        return AStore.makeYaml(this.instance, resourceRoot, path, hasDefault).prepare();
    }


    /**
     * Constructs JSON Store, and prepares it
     *
     * @param resourceRoot Root of the resource in binary
     * @param path       Path to resource
     * @param hasDefault Load default if not available in data folder
     * @return YAML Store
     * @throws Exception When something goes wrong
     */
    public @NotNull AStore provideJson(@Nullable String resourceRoot, @NotNull String path, boolean hasDefault) throws Exception {
        return AStore.makeJson(this.instance, resourceRoot, path, hasDefault).prepare();
    }


}
