package com.bstudio.ro_toolbox.service.resourceReplacer;

import com.bstudio.ro_toolbox.service.app.AppConfigService;
import com.bstudio.ro_toolbox.service.resourceReplacer.component.PackageHandler;
import com.bstudio.ro_toolbox.service.resourceReplacer.component.PackageManifestReader;
import com.bstudio.ro_toolbox.service.resourceReplacer.component.ResourcesUpdater;
import com.bstudio.ro_toolbox.service.resourceReplacer.model.IResourceReplacer;
import com.bstudio.ro_toolbox.service.resourceReplacer.utils.ICommonResourceMethods;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseIResourceReplacerResource
    implements IResourceReplacer, ICommonResourceMethods {
  @Autowired protected AppConfigService appConfigService;

  @Autowired protected ResourcesUpdater resourcesUpdater;

  @Autowired protected PackageManifestReader packageManifestReader;

  @Autowired protected PackageHandler packageHandler;
}
