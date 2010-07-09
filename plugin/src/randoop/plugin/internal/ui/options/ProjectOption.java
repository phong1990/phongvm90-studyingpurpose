package randoop.plugin.internal.ui.options;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import randoop.plugin.RandoopPlugin;
import randoop.plugin.internal.IConstants;
import randoop.plugin.internal.core.StatusFactory;
import randoop.plugin.internal.core.launching.IRandoopLaunchConfigurationConstants;
import randoop.plugin.internal.core.launching.RandoopArgumentCollector;
import randoop.plugin.internal.ui.launching.RandoopLaunchConfigurationUtil;

public class ProjectOption extends Option {
  private Shell fShell;
  
  private Text fProjectText;
  private IJavaProject fJavaProject;
  private Button fProjectBrowseButton;
  
  private Text fOutputSourceFolderText;
  private IPackageFragmentRoot fOutputSourceFolder;
  private Button fSourceFolderBrowseButton;
  
  public ProjectOption(Shell shell, Text projectText,
      Button projectBrowseButton, Text outputSourceFolderText,
      Button sourceFolderBrowseButton) {
    fShell = shell;
    
    fProjectText = projectText;
    fProjectText.setEditable(true);
    
    fProjectBrowseButton = projectBrowseButton;
    fProjectBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent evt) {
        handleProjectBrowseButtonSelected();
      }
    });

    fOutputSourceFolderText = outputSourceFolderText;
    fOutputSourceFolderText.setEditable(false);
    
    fSourceFolderBrowseButton = sourceFolderBrowseButton;
    fSourceFolderBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IPackageFragmentRoot chosenFolder = chooseSourceFolder();

        if (chosenFolder != null) {
          fOutputSourceFolder = chosenFolder;
          fOutputSourceFolderText.setText(fOutputSourceFolder.getElementName());
        }
      }
    });
    fSourceFolderBrowseButton.setEnabled(false);
  }
  
  public ProjectOption(Shell shell, IJavaProject project,
      Text outputSourceFolderText, Button sourceFolderBrowseButton) {
    fShell = shell;
    
    fJavaProject = project;
    
    fOutputSourceFolderText = outputSourceFolderText;
    fOutputSourceFolderText.setEditable(false);
    
    fSourceFolderBrowseButton = sourceFolderBrowseButton;
    fSourceFolderBrowseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IPackageFragmentRoot chosenFolder = chooseSourceFolder();

        if (chosenFolder != null) {
          fOutputSourceFolder = chosenFolder;
          fOutputSourceFolderText.setText(fOutputSourceFolder.getElementName());
        }
      }
    });
    fSourceFolderBrowseButton.setEnabled(true);
  }

  @Override
  public IStatus canSave() {
    if (fOutputSourceFolderText == null || fSourceFolderBrowseButton == null) {
      return StatusFactory.createErrorStatus("ProjectOption incorrectly initialized");
    }
  
    if (fJavaProject == null) {
      return StatusFactory.createErrorStatus("Project is not a valid Java project");
    }
    
    if (fOutputSourceFolder == null) {
      return StatusFactory.createErrorStatus("Output Directory is not a valid source folder");
    }
  
    return validate(fJavaProject.getElementName(), fOutputSourceFolder.getElementName());
  }

  public IStatus isValid(ILaunchConfiguration config) {
    String projectName = RandoopArgumentCollector.getProjectName(config);
    String outputSourceFolderName = RandoopArgumentCollector.getOutputDirectoryName(config);
  
    return validate(projectName, outputSourceFolderName);
  }

  /**
   * Returns an OK <code>IStatus</code> if the specified arguments could be
   * passed to Randoop without raising any error. If the arguments are not
   * valid, an ERROR status is returned with a message indicating what is wrong.
   * 
   * @param projectHandlerId
   * @param outputSourceFolderHandlerId
   * @return
   */
  protected static IStatus validate(String projectName, String outputSourceFolderName) {
    // see org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab.isValid
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IStatus status = workspace.validateName(projectName, IResource.PROJECT);

    IJavaProject javaProject;
    if (status.isOK()) {
      IProject project = workspace.getRoot().getProject(projectName);
      if (!project.exists()) {
        return StatusFactory.createErrorStatus(MessageFormat.format(
            "Project {0} does not exist", new String[] { projectName }));
      }
      if (!project.isOpen()) {
        return StatusFactory.createErrorStatus(MessageFormat.format(
            "Project {0} is closed", new String[] { projectName }));
      }
      
      try {
        javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
        if (javaProject == null) {
          return StatusFactory.createErrorStatus(MessageFormat.format(
              "Project {0} is not a Java project", new String[] { projectName }));
        }
      } catch (CoreException e) {
        RandoopPlugin.log(e);
        return StatusFactory.createErrorStatus();
      }
    } else {
      return StatusFactory.createErrorStatus(MessageFormat.format(
          "Illegal project name: {0}", new String[] { status.getMessage() }));
    }
    
    IPackageFragmentRoot outputDir = RandoopLaunchConfigurationUtil
        .getPackageFragmentRoot(javaProject, outputSourceFolderName);
    if (outputDir == null) {
      status = StatusFactory
          .createErrorStatus("Output Directory is not a valid source folder");
      return status;
    } else if (!outputDir.exists()) {
      status = StatusFactory.createErrorStatus("Output Directory "
          + outputDir.getElementName() + " does not exist");
      return status;
    } else if (!outputDir.getJavaProject().equals(javaProject)) {
      status = StatusFactory
          .createErrorStatus("Output Directory does not exist in project "
              + javaProject.getElementName());
      return status;
    }
  
    return StatusFactory.createOkStatus();
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    if (fProjectText != null) {
      String projectName = RandoopArgumentCollector.getProjectName(config);

      fJavaProject = RandoopLaunchConfigurationUtil.getProjectFromName(projectName);
      if (fJavaProject != null) {
        fProjectText.setText(fJavaProject.getElementName());
      }
      
      String attr = IRandoopLaunchConfigurationConstants.ATTR_PROJECT_NAME;
      notifyListeners(new OptionChangeEvent(attr, projectName));
    }

    if (fOutputSourceFolderText != null) {
      String folderName = RandoopArgumentCollector.getOutputDirectoryName(config);

      fOutputSourceFolder = RandoopLaunchConfigurationUtil.getPackageFragmentRoot(fJavaProject, folderName);
      if (fOutputSourceFolder != null) {
        fOutputSourceFolderText.setText(fOutputSourceFolder.getElementName());
      } else {
        fOutputSourceFolderText.setText(IConstants.EMPTY_STRING);
      }
      
      if (fSourceFolderBrowseButton != null) {
        fSourceFolderBrowseButton.setEnabled(fJavaProject != null);
      }
    }
  }
  
  // expects ILaunchConfigurationWorkingCopy
  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    if (fProjectText != null && fJavaProject != null)
      RandoopArgumentCollector.setProjectName(config, fProjectText.getText());
  
    if (fOutputSourceFolderText != null) {
      if (fOutputSourceFolder != null) {
        RandoopArgumentCollector.setOutputDirectoryName(config,
            fOutputSourceFolder.getElementName());
      } else {
        RandoopArgumentCollector.setOutputDirectoryName(config,
            IConstants.EMPTY_STRING);
      }
    }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    RandoopArgumentCollector.restoreProjectName(config);
    RandoopArgumentCollector.restoreOutputDirectoryName(config);
  }
  
  /*
   * Show a dialog that lets the user select a project. This in turn provides
   * context for the main type, allowing the user to key a main type name, or
   * constraining the search for main types to the specified project.
   * 
   * Copied from org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab
   */
  private void handleProjectBrowseButtonSelected() {
    IJavaProject project = chooseJavaProject();
    if (project == null) {
      return;
    }
    

    boolean okToProceed = true;
    // TODO: check if test inputs will change

    // It is okay to proceed if neither the output folder or selected types will
    // change
    if (!okToProceed) {
      okToProceed = MessageDialog.openQuestion(getShell(), "Change Project",
          "Changing the selected project will change some of the selected test inputs.\n\nOkay to proceed?");
    }
    
    if (okToProceed) {
      // TODO update selected test kinds
      
      fJavaProject = project;
      fProjectText.setText(fJavaProject.getElementName());
      
      fSourceFolderBrowseButton.setEnabled(true);
      
      // reset source folder if necessary
      if (fOutputSourceFolder != null) {
        String folder = fOutputSourceFolder.getElementName();
        fOutputSourceFolder = RandoopLaunchConfigurationUtil.getPackageFragmentRoot(fJavaProject, folder);
        if (fOutputSourceFolder == null) {
          if (fOutputSourceFolderText != null) {
            fOutputSourceFolderText.setText(IConstants.EMPTY_STRING);
          }
        }
      }
    }
    
    String attr = IRandoopLaunchConfigurationConstants.ATTR_PROJECT_NAME;
    notifyListeners(new OptionChangeEvent(attr, fProjectText.getText()));
  }
  
  /*
   * Realize a Java Project selection dialog and return the first selected
   * project, or null if there was none.
   * 
   * Copied from org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab
   */
  private IJavaProject chooseJavaProject() {
    IJavaProject[] projects;
    try {
      projects = JavaCore.create(getWorkspaceRoot()).getJavaProjects();
    } catch (JavaModelException e) {
      RandoopPlugin.log(e.getStatus());
      projects = new IJavaProject[0];
    }

    ILabelProvider labelProvider = new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT);
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), labelProvider);
    dialog.setTitle("Project Selection");
    dialog.setMessage("Choose a project to constrain the search for test classes:");
    dialog.setElements(projects);

    IJavaProject javaProject = getJavaProject();
    if (javaProject != null) {
      dialog.setInitialSelections(new Object[] { javaProject });
    }
    if (dialog.open() == Window.OK) {
      Object element = dialog.getFirstResult();
      if (element instanceof IJavaProject) {
        return (IJavaProject) element;
      }
    }
    return null;
  }

  /*
   * Return the IJavaProject corresponding to the project name in the project
   * name text field, or null if the text does not match a project name.
   * 
   * Copied from org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationTab
   */
  private IJavaProject getJavaProject() {
    String projectName = fProjectText.getText().trim();
    if (projectName.length() < 1) {
      return null;
    }
    return JavaCore.create(getWorkspaceRoot()).getJavaProject(projectName);
  }

  /*
   * Convenience method to get the workspace root.
   */
  private static IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }
  
  

  /**
   * Opens a selection dialog that allows to select a source container.
   * 
   * @return returns the selected package fragment root or <code>null</code> if
   *         the dialog has been canceled. The caller typically sets the result
   *         to the container input field.
   *         <p>
   *         Clients can override this method if they want to offer a different
   *         dialog.
   *         </p>
   */
  private IPackageFragmentRoot chooseSourceFolder() {
    IPackageFragmentRoot pfRoots[];
    try {
      pfRoots = fJavaProject.getPackageFragmentRoots();

      List<IPackageFragmentRoot> sourceFolders = new ArrayList<IPackageFragmentRoot>();
      for (IPackageFragmentRoot pfRoot : pfRoots) {
        try {
          if (pfRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
            sourceFolders.add(pfRoot);
          }
        } catch (JavaModelException e) {
          e.printStackTrace();
        }
      }

      ILabelProvider labelProvider = new JavaElementLabelProvider(
          JavaElementLabelProvider.SHOW_DEFAULT);
      ElementListSelectionDialog dialog = new ElementListSelectionDialog(
          getShell(), labelProvider);
      dialog.setTitle("Project Selection");
      dialog.setMessage("Choose a project to constrain the search for test classes:");
      dialog.setElements(sourceFolders
          .toArray(new IPackageFragmentRoot[sourceFolders.size()]));
      dialog.setHelpAvailable(false);

      if (dialog.open() == Window.OK) {
        Object element = dialog.getFirstResult();
        if (element instanceof IPackageFragmentRoot) {
          return (IPackageFragmentRoot) element;
        }
      }
    } catch (JavaModelException e) {
      RandoopPlugin.log(e.getStatus());
    }
    return null;
  }

  private Shell getShell() {
    return fShell;
  }
}