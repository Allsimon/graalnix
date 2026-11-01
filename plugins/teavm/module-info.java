/** Compiles a module to JavaScript with TeaVM, at its package step. */
module graalnix.teavm {
    requires build.jenesis;

    provides build.jenesis.BuildModuleFactory with
            graalnix.teavm.TeaVMFactory;
}
